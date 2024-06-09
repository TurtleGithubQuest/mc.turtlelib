package dev.turtle.turtlelib.gui

import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.TurtleGUI.InventorySlot

abstract class SlotBehavior(val name: String) {
    open var handleClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    fun register(gui: TurtleGUI) { gui.behaviors[name] = this@SlotBehavior }
}
abstract class SlotAction(val name: String) {
    open var onRun: (InventorySlot) -> Boolean = { false }
    fun register(gui: TurtleGUI) { gui.actions[name] = this@SlotAction }
}