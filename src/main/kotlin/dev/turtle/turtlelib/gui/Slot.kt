package dev.turtle.turtlelib.gui

import com.typesafe.config.Config
import com.typesafe.config.Config as TSConfig
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.InstancedGUI.InventorySlot

abstract class SlotAction(val name: String) {
    open var onClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    abstract var load: (InstancedGUI, InventorySlot, TSConfig) -> SlotAction
    fun register(gui: GUIBehavior) { gui.actions[name] = this }
}
abstract class SlotBehavior(val name: String) {
    open var onClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> false }
    abstract fun newInstance(): SlotBehavior
    abstract var load: (InstancedGUI, InventorySlot, TSConfig) -> SlotBehavior
    fun register(gui: GUIBehavior) { gui.behaviors[name] = this }
}
abstract class OpenSlotAction(name: String): SlotAction(name) {
    open var onLoad: (InstancedGUI, InventorySlot, TSConfig, SlotAction) -> SlotAction = { _, _, _, instance -> instance }
    open var beforeClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> true }
    open var afterClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> true }
}
abstract class OpenSlotBehavior(name: String): SlotBehavior(name) {
    open var onLoad: (InstancedGUI, InventorySlot, Config, SlotBehavior) -> SlotBehavior = { _, _, _, instance -> instance }
    open var beforeClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> true }
    open var afterClick: (InventorySlot, GUIClickEvent) -> Boolean = { _, _ -> true }
}