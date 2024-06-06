package dev.turtle.turtlelib.gui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.*
import com.comphenix.protocol.wrappers.ComponentConverter
import dev.turtle.turtlelib.TurtleLib.Companion.protocol
import dev.turtle.turtlelib.TurtlePlugin
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.InvocationTargetException
import java.util.*


abstract class TurtleGUI(val name: String, val turtle: TurtlePlugin, val language: String) {
    val title: String
    val stylizedTitle: MutableList<TextComponent>
    val background: String
    val size: Int
    val viewers = hashMapOf<UUID, TurtleGUIPlayer>()
    init {
        turtle.configFactory.get("lang")?.getSection("$language.gui.$name")!!.let { gui ->
            title = gui.getString("title")
            stylizedTitle = turtle.messageFactory.let { f ->
                f.newMessage(title).text().let { text -> f.run { text.parseTextComponents() }  }
            }
        }
        turtle.configFactory.get("config")?.getSection("gui.$name")!!.let { gui ->
            background = gui.getString("background") //todo
            size = gui.getInt("size").let {
                if (it % 9 != 0)
                    throw IllegalArgumentException("$name's inventory size($it) is not valid.")
                it
            }
        }
        turtle.guis[name] = this@TurtleGUI
        InventoryListener()
    }
    fun openFor(playerName: String): Boolean = openFor(Bukkit.getPlayer(playerName))
    fun openFor(p: Player?): Boolean {
        return p?.let {
            it.openInventory(Bukkit.createInventory(null, size, ""))
            sendTitlePacket(it)
            true
        }  ?: false
    }
    fun sendTitlePacket(player: Player?): Boolean {
        return player?.let { p ->
            val viewer = this@TurtleGUI.viewers[p.uniqueId]?: return false
            val windowId = viewer.windowId?: return false
            val owPacket = PacketContainer(PacketType.Play.Server.OPEN_WINDOW)
            owPacket.integers.write(0, windowId)
            owPacket.structures.write(0, viewer.windowType)
            owPacket.chatComponents.write(0, ComponentConverter.fromBaseComponent(*stylizedTitle.toTypedArray()))
            try {
                protocol.sendServerPacket(p, owPacket)
                true
            } catch (ex: InvocationTargetException) {
                ex.printStackTrace()
                false
            }
        }?: false
    }
    private inner class InventoryListener {
        init {
            protocol.addPacketListener(open())
            protocol.addPacketListener(close())
        }
        private fun open(): PacketListener {
            return object: PacketAdapter(turtle, ListenerPriority.HIGH, PacketType.Play.Server.OPEN_WINDOW) {
                override fun onPacketSending(e: PacketEvent) {
                    val windowId = e.packet.integers.read(0).takeIf { it != 0 }
                    e.packet.structures.readSafely(0).takeIf { it is InternalStructure }?.let { internalStructure ->
                        this@TurtleGUI.viewers[e.player.uniqueId] = TurtleGUIPlayer(windowId, internalStructure)
                    }
                }
            }
        }
        private fun close(): PacketListener {
            return object: PacketAdapter(turtle, ListenerPriority.HIGH, PacketType.Play.Client.CLOSE_WINDOW) {
                override fun onPacketReceiving(e: PacketEvent) {
                    this@TurtleGUI.viewers.remove(e.player.uniqueId)
                }
            }
        }
    }
    data class TurtleGUIPlayer(val windowId: Int?, val windowType: InternalStructure)
}