package dev.turtle.turtlelib.gui.open.slot.action

import com.typesafe.config.Config
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.gui.SlotAction
import dev.turtle.turtlelib.gui.InstancedGUI
import dev.turtle.turtlelib.gui.InstancedGUI.InventorySlot
import dev.turtle.turtlelib.gui.OpenSlotAction
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getStringOrNull
import org.bukkit.Bukkit

class CommandAction: OpenSlotAction(name="command") {
    var command: String = ""
    var executor: String = ""

    override var onClick: (InventorySlot, GUIClickEvent) -> Boolean = { slot, e ->
        if (beforeClick(slot, e)) {
            val turtle = e.gui.behavior.turtle
            val cs = if (executor == "SERVER") Bukkit.getConsoleSender() else e.player
            val command = turtle.messageFactory.newMessage(command).placeholder("player", e.player.name).text()
            Bukkit.getScheduler().runTask(turtle, Runnable {
                Bukkit.getServer().dispatchCommand(cs, command)
            })
            afterClick(slot, e)
        }
        true
    }
    override var load: (InstancedGUI, InventorySlot, Config) -> SlotAction = { gui, slot, cfg ->
        val instance = newInstance()
        instance.command = cfg.getString("command")
        instance.executor = cfg.getStringOrNull("executor").getValue()?:"PLAYER"
        if (command.startsWith("/"))
            command.drop(1)
        onLoad(gui, slot, cfg, instance)
    }
    override fun newInstance() = CommandAction()
}