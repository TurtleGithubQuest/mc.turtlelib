package dev.turtle.turtlelib.util

import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getAndTransform
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getIntOrNull
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getListOrNull
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getStringOrNull
import dev.turtle.turtlelib.util.extension.EnumExtension.valueOfOrNull
import dev.turtle.turtlelib.util.extension.TryCatch.tryOrNull
import dev.turtle.turtlelib.util.wrapper.CIMutableMap
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import java.util.UUID

class ItemFactory(internal val turtle: TurtlePlugin) {
    fun loadFromConfig(cfg: com.typesafe.config.Config): Item = Item().loadFromConfig(cfg, this)
}

class Item {
    var material: Material = Material.AIR
    var displayName: String = ""
    var _displayName: String = ""
        private set
    var lore = listOf<String>()
    var _lore = listOf<String>()
        private set
    var amount = 1
    var damage: Int? = null
    var owningPlayer: UUID? = null
    fun loadFromConfig(cfg: com.typesafe.config.Config, itemFactory: ItemFactory) = apply {
        val m = itemFactory.turtle.messageFactory
        _displayName = cfg.getStringOrNull("display_name").getValue()?:""
        _lore = cfg.getListOrNull<String>("lore").getValue()?:listOf()
        loadPlaceholders(m, CIMutableMap())
        cfg.getIntOrNull("amount").getValue()?.let { amount = it }
        material = cfg.getStringOrNull("material").getValue()?.let { valueOfOrNull<Material>(it, true) }?: Material.AIR
        damage = cfg.getIntOrNull("damage").getValue()
        owningPlayer = cfg.getAndTransform("owning_player") { it.tryOrNull{UUID.fromString(it.toString())} }.getValue()
    }
    fun loadPlaceholders(m: MessageFactory, placeholders: CIMutableMap<Any>) = apply {
        val pl = HashMap(placeholders)
        displayName = m.newMessage(_displayName).placeholders(pl).disablePlaceholders(pl.isEmpty()).text()
        lore = _lore.map { line -> m.newMessage(line).placeholders(pl).disablePlaceholders(pl.isEmpty()).text() }
    }
    val itemStack get() = run {
        val its = ItemStack(material)
        its.amount = amount
        its.itemMeta?.let { itm ->
            itm.setDisplayName(displayName)
            itm.lore = lore
            when (itm) { //todo
                is SkullMeta -> { owningPlayer?.let { playerID ->
                    val p = Bukkit.getPlayer(playerID)?:Bukkit.getOfflinePlayer(playerID)
                    itm.setOwningPlayer(p)
                }}
                is Damageable -> { damage?.let { itm.damage = it} }
                /*is BannerMeta -> {}
                is BlockStateMeta -> {}
                is BookMeta -> {}
                is CrossbowMeta -> {}
                is EnchantmentStorageMeta -> {}
                is FireworkEffectMeta -> {}
                is FireworkMeta -> {}
                is LeatherArmorMeta -> {}
                is MapMeta -> {}
                is PotionMeta -> {}
                is SuspiciousStewMeta -> {}
                is TropicalFishBucketMeta -> {}*/
            }
        its.itemMeta = itm}
        its
    }
}