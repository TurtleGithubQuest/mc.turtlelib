package dev.turtle.turtlelib.gui

import com.typesafe.config.Config as TSConfig
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.TurtleGUI.InventorySlot

abstract class SlotAction(val name: String) {
    open var onRun: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    abstract var load: (TurtleGUI, InventorySlot, TSConfig) -> SlotAction
    fun register(gui: TurtleGUI) { gui.actions[name] = this }
}
abstract class SlotBehavior(val name: String) {
    open var handleClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    abstract var load: (TurtleGUI, InventorySlot, TSConfig) -> SlotBehavior
    fun register(gui: TurtleGUI) { gui.behaviors[name] = this }
}