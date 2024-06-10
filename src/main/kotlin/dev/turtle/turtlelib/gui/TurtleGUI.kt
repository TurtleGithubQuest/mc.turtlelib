package dev.turtle.turtlelib.gui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.*
import com.comphenix.protocol.wrappers.ComponentConverter
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigObject
import dev.turtle.turtlelib.TurtleLib.Companion.protocol
import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.util.CIMutableMap
import dev.turtle.turtlelib.util.Item
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getListOrNull
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getStringOrNull
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.lang.reflect.InvocationTargetException
import java.util.*
import com.typesafe.config.Config as TypeSafeConfig

data class InstancedGUI(
    var name: String, var behavior: GUIBehavior
) {
    var title: String = behavior.name
    var stylizedTitle = behavior.stylizedTitle?:mutableListOf()
    var size: Int = behavior.size?: 27
    val viewers = hashMapOf<UUID, TurtleGUIPlayer>()
    val viewRequests = hashSetOf<UUID>()
    var globalInventory: Inventory? = null
    val content = hashMapOf<Int, InventorySlot>()
    fun onRegister(): Boolean { InventoryListener(); return behavior.onGUIRegister() }
    fun getViewer(uuid: UUID): TurtleGUIPlayer? = this.viewers[uuid]
    fun openFor(playerName: String): Boolean = openFor(Bukkit.getPlayer(playerName))
    fun openFor(player: Player?): Boolean {
        return player?.let { p ->
            viewRequests.add(p.uniqueId)
            val inv = getViewer(p.uniqueId)?.getInventory()?:createInventory()
            p.openInventory(inv)
            sendUpdatedInventory(p)
            true
        }?: false
    }
    fun createInventory(): Inventory {
        val inv = Bukkit.createInventory(null, size, "")
        content.forEach { (_, slot) -> slot.prepareSlot(inv) }
        return inv
    }
    fun sendUpdatedInventory(player: Player?): Boolean {
        return player?.let { p ->
            val viewer = getViewer(p.uniqueId)?: return false
            val windowId = viewer.windowId
            val owPacket = PacketContainer(PacketType.Play.Server.OPEN_WINDOW)
            owPacket.integers.write(0, windowId)
            owPacket.structures.write(0, viewer.windowType)
            owPacket.chatComponents.write(0, ComponentConverter.fromBaseComponent(*stylizedTitle.toTypedArray()))
            val wiPacket = PacketContainer(PacketType.Play.Server.WINDOW_ITEMS)
            wiPacket.integers.write(0, windowId)
            wiPacket.itemListModifier.write(0, p.openInventory.topInventory.contents.map{ it?:ItemStack(Material.AIR) }) //contents.filterNotNull() removes the item completely, although we need to fill null slots in order to preserve indexes
            try {
                protocol.sendServerPacket(p, owPacket)
                protocol.sendServerPacket(p, wiPacket)
                p.updateInventory() //Maybe also send packet using ProtocolLib instead of calling this method.
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
            protocol.addPacketListener(click())
        }
        private fun open(): PacketListener {
            return object: PacketAdapter(behavior.turtle, ListenerPriority.HIGH, PacketType.Play.Server.OPEN_WINDOW) {
                override fun onPacketSending(e: PacketEvent) {
                    // Is packet related to this GUI ?
                    if (!viewRequests.remove(e.player.uniqueId)) return
                    // The inventory with ID 0 belongs to a player, we don't manage that.
                    val windowId = e.packet.integers.read(0).takeIf { it != 0 }?: return
                    e.packet.structures.readSafely(0).takeIf { it is InternalStructure }?.let { internalStructure ->
                        this@InstancedGUI.viewers[e.player.uniqueId] = TurtleGUIPlayer(windowId, internalStructure, e.player.openInventory.topInventory)
                    }
                }
            }
        }
        private fun close(): PacketListener {
            return object: PacketAdapter(behavior.turtle, ListenerPriority.HIGH, PacketType.Play.Client.CLOSE_WINDOW) {
                override fun onPacketReceiving(e: PacketEvent) {
                    this@InstancedGUI.viewers.remove(e.player.uniqueId)
                }
            }
        }
        private fun click(): PacketListener {
            return object: PacketAdapter(behavior.turtle, ListenerPriority.HIGH, PacketType.Play.Client.WINDOW_CLICK) {
                override fun onPacketReceiving(e: PacketEvent) {
                    val viewer = getViewer(e.player.uniqueId)?: return
                    val eventWindowId = e.packet.integers.read(0).takeIf { it != 0 }?: return
                    if (viewer.windowId != eventWindowId) return
                    this@InstancedGUI.onClick(GUIClickEvent(this@InstancedGUI, viewer, e.player, e))
                }
            }
        }
    }
    fun onClick(e: GUIClickEvent): Boolean =
       (this@InstancedGUI.content[e.click.slot]?:this@InstancedGUI.content[-1])?.onClick(e) ?: false

    inner class TurtleGUIPlayer(val windowId: Int, val windowType: InternalStructure, var window: Inventory) {
        /** GUIPlayer's viewed inventory */
        fun getInventory(): Inventory = globalInventory?: window
    }
    /**
     * Please note that `index` may not correspond directly to the clicked slot id.
     * For instance, an `index` value of '-1' might be used to represent a default slot.
     */
    inner class InventorySlot(var index: Int) {
        var actions: Array<SlotAction> = arrayOf()
        lateinit var behavior: SlotBehavior
        lateinit var item: Item
        fun prepareSlot(inventory: Inventory) {
            if (index == -1) //default slot
                inventory.forEachIndexed{ index, itemStack ->
                    itemStack?:inventory.setItem(index, item.itemStack) }
            else
                inventory.setItem(index, item.itemStack)
        }
        fun addActions(value: Array<SlotAction>) = apply { actions += value }
        fun onClick(e: GUIClickEvent): Boolean = behavior.handleClick(this, e) && actions.none { action -> action.onRun(this, e) }
        fun register(cfg: Config) = apply {
            val behaviorName = cfg.getString("behavior")
            val m = this@InstancedGUI.behavior.turtle.messageFactory
            this@InstancedGUI.behavior.behaviors[behaviorName]?.let { behavior ->
                try {
                    this@InventorySlot.behavior = behavior.load(this@InstancedGUI, this@InventorySlot, cfg.withoutPath("actions"))
                } catch (ex: ConfigException) {
                    val message = "&dTurtleGUI&7: &cFailed&7 to load slot behavior '&e$behaviorName&7'. ${ex.message}"
                    m.newMessage(message).send()
                }
            }?: this@InstancedGUI.behavior.turtle.disable("&dTurtleGUI&7: Slot behavior '&e$behaviorName&7' &cnot found &7in GUI '&e${behavior.name}&7'.")
            cfg.getListOrNull<ConfigObject>("actions").getValue()?.forEach {
                val actionConfig = it.toConfig()
                val actionType = actionConfig.getString("type")
                try {
                    this@InstancedGUI.behavior.actions[actionType]?.let { action -> this@InventorySlot.actions += action.load(this@InstancedGUI, this@InventorySlot, actionConfig)
                    }?: m.newMessage("&dTurtleGUI&7: Slot action '&e$actionType&7' &cnot found &7in GUI '&e$name&7'.").send()
                } catch (ex: ConfigException) {
                    val message = "&dTurtleGUI&7: &cFailed&7 to load slot action '&e$actionType&7'. ${ex.message}"
                    m.newMessage(message).send()
                }
            }
            item = this@InstancedGUI.behavior.turtle.itemFactory.loadFromConfig(cfg)
        }
    }
}

abstract class GUIBehavior(val name: String, val turtle: TurtlePlugin) {
    var stylizedTitle: List<TextComponent>? = null
    var size: Int? = null
    val behaviors = CIMutableMap<SlotBehavior>()
    val actions = CIMutableMap<SlotAction>()
    abstract fun onGUIRegister(): Boolean
}