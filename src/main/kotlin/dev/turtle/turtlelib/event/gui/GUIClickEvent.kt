package dev.turtle.turtlelib.event.gui

import com.comphenix.protocol.events.PacketEvent
import dev.turtle.turtlelib.gui.TurtleGUI
import org.bukkit.inventory.Inventory
import dev.turtle.turtlelib.event.gui.InventoryClickKeyCategory.*
import dev.turtle.turtlelib.event.gui.InventoryClickType.*

class GUIClickEvent(val gui: TurtleGUI, val viewer: TurtleGUI.TurtleGUIPlayer, val event: PacketEvent) {
    fun setCancelled(value: Boolean=true) = apply {event.isCancelled=value}
    val click = Click()
    inner class Click {
        val inventory: Inventory = viewer.getInventory()
        val slot = event.packet.integers.read(2)
        val button = event.packet.integers.readSafely(3)
        val type = event.packet.modifier.readSafely(4).toString()
        // maybe add try-catch, idk
        val mode = InventoryClickType.valueOf(type)
        val key = InventoryClickKey.valueOf(mode, button)
    }
}
/**<a href="https://wiki.vg/Protocol#Click_Container_Button">https://wiki.vg/Protocol#Click_Container_Button</a>*/
enum class InventoryClickKey(val mode: InventoryClickType, val button: Int, vararg categories: InventoryClickKeyCategory) {
    LEFT_CLICK(PICKUP, 0, LEFT, MOUSE),
    RIGHT_CLICK(PICKUP,1, RIGHT, MOUSE),
    DROP_SINGLE(PICKUP,0, InventoryClickKeyCategory.DROP, MOUSE),
    DROP_STACK(PICKUP,1, InventoryClickKeyCategory.DROP, MOUSE),
    SHIFT_LEFT_CLICK(QUICK_MOVE, 0, LEFT, MOUSE, SHIFT),
    SHIFT_RIGHT_CLICK(QUICK_MOVE, 1, RIGHT, MOUSE, SHIFT),
    NUMBER_1(SWAP, 0, NUMBER, KEYBOARD),
    NUMBER_2(SWAP, 1, NUMBER, KEYBOARD),
    NUMBER_3(SWAP, 2, NUMBER, KEYBOARD),
    NUMBER_4(SWAP, 3, NUMBER, KEYBOARD),
    NUMBER_5(SWAP, 4, NUMBER, KEYBOARD),
    NUMBER_6(SWAP, 5, NUMBER, KEYBOARD),
    NUMBER_7(SWAP, 6, NUMBER, KEYBOARD),
    NUMBER_8(SWAP, 7, NUMBER, KEYBOARD),
    NUMBER_9(SWAP, 8, NUMBER, KEYBOARD),
    SWAP_SLOT_INDEX(SWAP, -1, NUMBER, KEYBOARD, ILLEGAL), /**Impossible in vanilla clients*/
    OFFHAND_SWAP(SWAP, 40, NUMBER, KEYBOARD),
    MIDDLE_CLICK_CREATIVE(CLONE, 2, MIDDLE, MOUSE, CREATIVE), /**Middle click, only defined for creative players in non-player inventories.*/
    DROP(THROW, 0, InventoryClickKeyCategory.DROP, KEYBOARD), /**(Clicked item is always empty)*/
    CTRL_DROP(THROW, 0, InventoryClickKeyCategory.DROP, KEYBOARD), /**(Clicked item is always empty)*/
    DRAG_LEFT_START(QUICK_CRAFT, 0, LEFT, DRAG, MOUSE),
    DRAG_LEFT(QUICK_CRAFT, 1, LEFT, DRAG, MOUSE),
    DRAG_LEFT_END(QUICK_CRAFT, 2, LEFT, DRAG, MOUSE),
    DRAG_RIGHT_START(QUICK_CRAFT, 4, RIGHT, DRAG, MOUSE),
    DRAG_RIGHT(QUICK_CRAFT, 5, RIGHT, DRAG, MOUSE),
    DRAG_RIGHT_END(QUICK_CRAFT, 6, RIGHT, DRAG, MOUSE),
    DRAG_MIDDLE_START(QUICK_CRAFT, 8, MIDDLE, DRAG, MOUSE, CREATIVE), /**only defined for creative players in non-player inventories*/
    DRAG_MIDDLE(QUICK_CRAFT, 9, MIDDLE, DRAG, MOUSE, CREATIVE), /**only defined for creative players in non-player inventories*/
    DRAG_MIDDLE_END(QUICK_CRAFT, 10, MIDDLE, DRAG, MOUSE, CREATIVE), /**only defined for creative players in non-player inventories*/
    DOUBLE_CLICK(PICKUP_ALL, 0, MOUSE),
    DOUBLE_CLICK_REVERSE(PICKUP_ALL, 1, MOUSE, ILLEGAL); /**Impossible in vanilla clients*/
    val categories = setOf(*categories)
    fun hasCategory(value: InventoryClickKeyCategory): Boolean = value in categories

    companion object {
        private val map = entries.associateBy { it.mode to it.button }
        fun valueOf(mode: InventoryClickType, button: Int): InventoryClickKey? = map[Pair(mode, button)]?: run {
            if (mode == SWAP) SWAP_SLOT_INDEX
            else null
        }
    }
}
enum class InventoryClickType {
    PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL
}
enum class InventoryClickKeyCategory {
    LEFT, RIGHT, MIDDLE, NUMBER, DRAG, MOUSE, DROP, SHIFT, KEYBOARD, ILLEGAL, CREATIVE
}