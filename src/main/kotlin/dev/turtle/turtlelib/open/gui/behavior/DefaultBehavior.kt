package dev.turtle.turtlelib.open.gui.behavior

import com.typesafe.config.Config
import dev.turtle.turtlelib.event.gui.GUIClickEvent
import dev.turtle.turtlelib.event.gui.InventoryClickKey
import dev.turtle.turtlelib.event.gui.InventoryClickKeyCategory
import dev.turtle.turtlelib.gui.SlotBehavior
import dev.turtle.turtlelib.gui.InstancedGUI
import dev.turtle.turtlelib.gui.InstancedGUI.InventorySlot
import dev.turtle.turtlelib.gui.OpenSlotBehavior
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getBooleanOrNull

open class DefaultBehavior(name: String="default"): OpenSlotBehavior(name) {
    var movement: Boolean = true
        private set
    var move_drop: Boolean = false
        private set
    var move_player: Boolean = false
        private set
    var move_gui: Boolean = false
        private set

    override var onClick: (InventorySlot, GUIClickEvent) -> Boolean = { slot, e ->
        if (beforeClick(slot, e)) {
            val slotIndex = e.click.slot
            val key = e.click.key ?: InventoryClickKey.SHIFT_RIGHT_CLICK
            val isOtherInv = key.hasCategory(InventoryClickKeyCategory.OTHER_INV)
            val isSourceGUI = slotIndex < e.gui.size && slotIndex > -1
            val isSourcePlayer = slotIndex > e.gui.size
            if (movement)
                when {
                    !move_drop && key.hasCategory(InventoryClickKeyCategory.DROP) -> //DROP
                        if (isSourceGUI) !move_gui else if (isSourcePlayer) !move_player else !move_drop

                    !move_gui && isSourceGUI || !move_player && isOtherInv -> //SOURCE: GUI
                        if (isOtherInv) !move_gui || !move_player //GUI -> PLAYER
                        else !move_gui //GUI -> GUI
                    !move_player && isSourcePlayer || !move_gui && isOtherInv -> //SOURCE: PLAYER
                        if (isOtherInv) !move_player || !move_gui // PLAYER -> GUI
                        else !move_player // PLAYER -> PLAYER
                    else -> null
                }?.let { e.setCancelled(it) }
            else e.setCancelled(true)
            afterClick(slot, e)
        }
        true
    }
    override var load: (InstancedGUI, InventorySlot, Config) -> SlotBehavior = { gui, slot, cfg ->
        val instance = newInstance()
        instance.movement = cfg.getBooleanOrNull("move.allow").getValue()?:true
        instance.move_drop = cfg.getBooleanOrNull("move.drop").getValue()?:false
        instance.move_player = cfg.getBooleanOrNull("move.player").getValue()?:false
        instance.move_gui = cfg.getBooleanOrNull("move.gui").getValue()?:false
        onLoad(gui, slot, cfg, instance)
    }
    override fun newInstance() = DefaultBehavior()
}