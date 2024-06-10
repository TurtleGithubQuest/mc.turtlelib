package dev.turtle.turtlelib.template.gui.action

import com.typesafe.config.Config
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.SlotAction
import dev.turtle.turtlelib.gui.InstancedGUI
import dev.turtle.turtlelib.gui.InstancedGUI.InventorySlot

class OpenWindowAction: SlotAction(name="opengui") {
    var targetWindow: InstancedGUI? = null
        private set

    override var onRun: (InventorySlot, GUIClickEvent) -> Boolean = { slot, e ->
        targetWindow?.openFor(e.player)
        true
    }
    override var load: (InstancedGUI, InventorySlot, Config) -> SlotAction = { gui, slot, cfg ->
        val instance = OpenWindowAction()
        val windowName = cfg.getString("gui").toString()
        instance.targetWindow = gui.behavior.turtle.guiFactory.instances[windowName]
        instance
    }
}