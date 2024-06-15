package dev.turtle.turtlelib.gui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.*
import com.comphenix.protocol.wrappers.ComponentConverter
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import dev.turtle.turtlelib.TurtleLib.Companion.protocol
import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.util.wrapper.CIMutableMap
import dev.turtle.turtlelib.util.Item
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getListOrNull
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.lang.reflect.InvocationTargetException
import java.util.*

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
    fun onRegister(instance: InstancedGUI, cfg: Config): Boolean { InventoryListener(); return behavior.onRegister(instance, cfg) }
    fun getViewer(uuid: UUID): TurtleGUIPlayer? = this.viewers[uuid]
    fun openFor(playerName: String): Boolean = openFor(Bukkit.getPlayer(playerName))

    fun openFor(player: Player?): Boolean {
        if (player == null) return false
        if (!behavior.beforeOpen(this, player)) return false
        Bukkit.getScheduler().runTask(behavior.turtle, Runnable {
            closeInventoryAsync(player)
            viewRequests.add(player.uniqueId)
            val inv = getViewer(player.uniqueId)?.getInventory() ?: createInventory()
            player.openInventory(inv)?.let {
                sendUpdatedInventory(player)
            }
        })
        return true
    }
    fun createInventory(): Inventory {
        val inv = Bukkit.createInventory(null, size, "")
        content.forEach { (_, slot) -> slot.updateSlot(inv) }
        return inv
    }
    fun closeInventoryAsync(player: Player?) {
        player?.let { p ->
            val cwPacket = PacketContainer(PacketType.Play.Server.CLOSE_WINDOW)
            val viewer = getViewer(p.uniqueId)?: return
            val windowId = viewer.windowId
            cwPacket.integers.write(0, windowId)
            try {
                protocol.sendServerPacket(p, cwPacket)
            } catch (ex: InvocationTargetException) { ex.printStackTrace() }
        }
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
                Bukkit.getScheduler().runTask(behavior.turtle, Runnable {
                    protocol.sendServerPacket(p, owPacket)
                    protocol.sendServerPacket(p, wiPacket)
                    p.updateInventory() //Maybe also send packet using ProtocolLib instead of calling this method.
                })
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
                    if (this@InstancedGUI.behavior.onOpen(this@InstancedGUI))
                        e.packet.structures.readSafely(0).takeIf { it is InternalStructure }?.let { internalStructure ->
                            this@InstancedGUI.viewers[e.player.uniqueId] = TurtleGUIPlayer(windowId, internalStructure, e.player.openInventory.topInventory)
                        }
                    else e.isCancelled = true
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
    inner class InventorySlot(var index: Int) {
        var actions: Array<SlotAction> = arrayOf()
        lateinit var behavior: SlotBehavior
        lateinit var item: Item
        fun updateSlot(inventory: Inventory, force: Boolean = false) {
            /*if (index == -1) //default slot
                inventory.forEachIndexed{ index, itemStack ->
                    itemStack?:inventory.setItem(index, item.itemStack) }
            else*/
            if (force || (item.material != Material.AIR && index >= 0 && size > index))
                inventory.setItem(index, item.itemStack)
        }
        fun addActions(value: Array<SlotAction>) = apply { actions += value }
        fun onClick(e: GUIClickEvent): Boolean = behavior.onClick(this, e) && actions.none { action -> action.onClick(this, e) }
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
            }?: this@InstancedGUI.behavior.turtle.disable("&dTurtleGUI&7: Slot behavior '&e$behaviorName&7' is &cnot registered &7in GUI '&e${this@InstancedGUI.behavior.name}&7'.")
            cfg.getListOrNull<HashMap<String, Any>>("actions").getValue()?.forEach {
                val actionConfig = ConfigFactory.parseMap(it)
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
    open fun onRegister(instance: InstancedGUI, cfg: Config): Boolean = true
    open fun beforeOpen(instance: InstancedGUI, player: Player): Boolean = true
    open fun onOpen(instance: InstancedGUI): Boolean = true
}