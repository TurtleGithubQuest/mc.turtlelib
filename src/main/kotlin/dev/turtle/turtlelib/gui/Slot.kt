package dev.turtle.turtlelib.gui

import com.typesafe.config.Config as TSConfig
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.InstancedGUI.InventorySlot

abstract class SlotAction(val name: String) {
    open var onRun: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    abstract var load: (InstancedGUI, InventorySlot, TSConfig) -> SlotAction
    fun register(gui: GUIBehavior) { gui.actions[name] = this }
}
abstract class SlotBehavior(val name: String) {
    open var handleClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    abstract var load: (InstancedGUI, InventorySlot, TSConfig) -> SlotBehavior
    fun register(gui: GUIBehavior) { gui.behaviors[name] = this }
}