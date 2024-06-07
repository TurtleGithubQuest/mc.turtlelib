package dev.turtle.turtlelib.gui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.*
import com.comphenix.protocol.wrappers.ComponentConverter
import com.typesafe.config.Config
import com.typesafe.config.ConfigObject
import com.typesafe.config.Config as TypeSafeConfig
import dev.turtle.turtlelib.TurtleLib.Companion.protocol
import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.util.CIMutableMap
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.lang.reflect.InvocationTargetException
import java.util.*


abstract class TurtleGUI(val name: String, val turtle: TurtlePlugin) {
    var title: String = ""
    var stylizedTitle = mutableListOf<TextComponent>()
        private set
    var size: Int = 27
    val viewers = hashMapOf<UUID, TurtleGUIPlayer>()
    var globalInventory: Inventory? = null
    val content = hashMapOf<Int, InventorySlot>()
    val behaviors = CIMutableMap<TurtleGUI.SlotBehavior>()
    val actions = CIMutableMap<TurtleGUI.SlotAction>()
    fun loadFromConfig(langSection: TypeSafeConfig, guiSection: TypeSafeConfig) {
        langSection.let { gui ->
            title = gui.getString("title")
            stylizedTitle = turtle.messageFactory.let { f ->
                f.newMessage(title).text().let { text -> f.run { text.parseTextComponents() }  }
            }
        }
        guiSection.let { gui ->
            size = gui.getInt("size").let {
                if (it % 9 != 0)
                    throw IllegalArgumentException("$name's inventory size($it) is not valid.")
                it
            }
            gui.getConfig("inventory").let { inv ->
                inv.root().forEach { (indexStr, value) ->
                    (value as com.typesafe.config.ConfigObject).toConfig().let { slot ->
                        indexStr.toIntOrNull()?.let { index ->
                            content[index] = InventorySlot(index).loadFromConfig(slot)
                        }?: turtle.messageFactory.newMessage("&dTurtleGUI&7: Invalid index '&e$indexStr&7' in &e$name&7.").send()
                    }
                }
            }
            if (gui.getBoolean("global")) {
                globalInventory = createInventory()
            }
        }
        turtle.eventListeners.add(InventoryListener().EventListener())
        turtle.guis[name] = this@TurtleGUI
    }
    fun getViewer(uuid: UUID): TurtleGUIPlayer? = this@TurtleGUI.viewers[uuid]
    fun createInventory(): Inventory {
        val inv = Bukkit.createInventory(null, size, "")
        content.forEach { (_, slot) -> slot.prepareSlot(inv) }
        return inv
    }
    fun openFor(playerName: String): Boolean = openFor(Bukkit.getPlayer(playerName))
    fun openFor(player: Player?): Boolean {
        return player?.let { p->
            val inv = getViewer(p.uniqueId)?.getInventory()?:createInventory()
            p.openInventory(inv)
            sendTitlePacket(p)
        }?: false
    }
    fun sendTitlePacket(player: Player?): Boolean {
        return player?.let { p ->
            val viewer = getViewer(p.uniqueId)?: return false
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
                        this@TurtleGUI.viewers[e.player.uniqueId] = TurtleGUIPlayer(windowId, internalStructure, e.player.openInventory.topInventory)
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
        inner class EventListener: Listener {
            @EventHandler
            fun onClick(e: InventoryClickEvent) { //todo: Check inventory
                println("Clicked "+e.slot)
                this@TurtleGUI.onClick(e)
            }
        }
    }
    fun onClick(e: InventoryClickEvent) = this@TurtleGUI.content[e.slot]?.onClick(e)?:false
    inner class TurtleGUIPlayer(val windowId: Int?, val windowType: InternalStructure, var window: Inventory) {
        fun getInventory(): Inventory = globalInventory?: window
    }
    inner class InventorySlot(var index: Int) {
        var actions: Array<SlotAction> = arrayOf()
        lateinit var behavior: SlotBehavior
        var material: Material = Material.STONE
        fun prepareSlot(inventory: Inventory) {
            inventory.setItem(index, ItemStack(material))
        }
        fun loadFromConfig(cfg: Config) = apply {
            val behaviorName = cfg.getString("behavior")
            this@TurtleGUI.behaviors[behaviorName]?.let { behavior ->
                this@InventorySlot.behavior = behavior
            }?: turtle.disable("&dTurtleGUI&7: Slot behavior '&e$behaviorName&7' &cnot found &7in GUI '&e$name&7'.")
            cfg.getList("actions").forEach {
                val actionType = (it as ConfigObject).toConfig().getString("type")
                this@TurtleGUI.actions[actionType]?.let { action -> this@InventorySlot.actions += action}
            }
        }
        fun addActions(value: Array<SlotAction>) = apply { actions += value }
        fun onClick(e: InventoryClickEvent): Boolean {
            actions.forEach { action ->
                action.onRun(this)
            }
            return behavior.handleClick(this, e)
        }
    }
    abstract inner class SlotBehavior(val name: String) {
        open var handleClick: (InventorySlot, InventoryClickEvent) -> Boolean = { slot, e -> false }
        init { this@TurtleGUI.behaviors[name] = this@SlotBehavior }
    }
    abstract inner class SlotAction(val name: String) {
        open var onRun: (InventorySlot) -> Boolean = { false }
        init { this@TurtleGUI.actions[name] = this@SlotAction }
    }
}