package dev.turtle.turtlelib.gui.open.slot.action

import com.typesafe.config.Config
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.SlotAction
import dev.turtle.turtlelib.gui.InstancedGUI
import dev.turtle.turtlelib.gui.InstancedGUI.InventorySlot
import dev.turtle.turtlelib.gui.OpenSlotAction

class OpenWindowAction: OpenSlotAction(name="opengui") {
    var targetWindow: InstancedGUI? = null

    override var onClick: (InventorySlot, GUIClickEvent) -> Boolean = { slot, e ->
        if (beforeClick(slot, e)) {
            targetWindow?.openFor(e.player)
            afterClick(slot, e)
        }
        true
    }
    override var load: (InstancedGUI, InventorySlot, Config) -> SlotAction = { gui, slot, cfg ->
        val instance = newInstance()
        val windowName = cfg.getString("gui").toString()
        instance.targetWindow = gui.behavior.turtle.guiFactory.instances[windowName]
        onLoad(gui, slot, cfg, instance)
    }
    override fun newInstance() = OpenWindowAction()
}