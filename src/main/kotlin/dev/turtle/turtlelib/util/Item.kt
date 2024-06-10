package dev.turtle.turtlelib.util

import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getIntOrNull
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getListOrNull
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getStringOrNull
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class ItemFactory(internal val turtle: TurtlePlugin) {
    fun loadFromConfig(cfg: com.typesafe.config.Config): Item = Item().loadFromConfig(cfg, this)
}

class Item {
    var material: Material = Material.AIR
    var displayName: String = ""
    var lore = listOf<String>()
    var amount = 1
    fun loadFromConfig(cfg: com.typesafe.config.Config, itemFactory: ItemFactory) = apply {
        val m = itemFactory.turtle.messageFactory
        displayName = m.newMessage(cfg.getStringOrNull("display_name").getValue()?:"").text()
        lore = cfg.getListOrNull<String>("lore").getValue()?.map { line -> m.newMessage(line).text() }?:listOf()
        cfg.getIntOrNull("amount").getValue()?.let { amount = it }
        material = cfg.getStringOrNull("material").getValue()?.let { Material.valueOf(it) }?: Material.AIR
    }
    val itemStack get() = run {
        val its = ItemStack(material)
        its.amount = amount
        its.itemMeta?.let { itm ->
            itm.setDisplayName(displayName)
            itm.lore = lore
        its.itemMeta = itm}
        its
    }
}