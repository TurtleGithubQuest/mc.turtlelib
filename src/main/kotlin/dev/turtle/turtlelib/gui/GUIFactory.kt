package dev.turtle.turtlelib.gui

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getListOrNull
import dev.turtle.turtlelib.util.wrapper.CIMutableMap
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getStringOrNull
import dev.turtle.turtlelib.util.extension.TryCatch.tryOrNull

class GUIFactory(internal val turtle: TurtlePlugin) {
    val instances = CIMutableMap<InstancedGUI>()
    var behaviors = CIMutableMap<GUIBehavior>()
    fun InstancedGUI.register() = run { instances[this.name] = this }
    fun registerBehaviors(vararg types: GUIBehavior): Int = types.count { behaviors[it.name] = it; true }
    fun loadFromConfig(name: String,
                 cfg: Config =turtle.configFactory.get("config")?.getSection("gui.$name")!!): InstancedGUI? {
        val m = turtle.messageFactory
        return behaviors[cfg.getStringOrNull("behavior").getValue().toString()]?.let { behavior ->
            InstancedGUI(name, behavior).apply {
                cfg.let { gui ->
                    gui.getStringOrNull("title").getValue()?.let {
                        title = it
                        stylizedTitle = m.let { f ->
                            f.newMessage(title).text().let { text -> f.run { text.parseTextComponents() }  }
                        }
                    }
                    size = gui.getInt("size").let {
                        if (it % 9 != 0)
                            throw IllegalArgumentException("$name's inventory size($it) is not valid.")
                        it
                    }
                    gui.getConfig("inventory").let { inv ->
                        inv.root().forEach { (indexStr, value) ->
                            indexStr.toIntOrNull()?.let{ index -> value.tryOrNull{ (it as com.typesafe.config.ConfigObject).toConfig() }
                                ?.let { slotConfig -> content[index] = InventorySlot(index).register(slotConfig) }
                            }
                        }
                        inv.getListOrNull<HashMap<String, Any>>("fill").getValue()?.let {
                            for (hashMap in it) {
                                val fillConfig = ConfigFactory.parseMap(hashMap)
                                fillConfig.getStringOrNull("parent_slot").getValue()?.let { dataParent ->
                                    inv.tryOrNull { inv.getConfig(dataParent) }?.let { slotConfig ->
                                        fillConfig.getListOrNull<Int>("slots").getValue()?.let { slots ->
                                            slots.forEach { index ->
                                                if (content[index] == null)
                                                    content[index] = InventorySlot(index).register(slotConfig)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        inv.tryOrNull{inv.getObject("default")}?.let {
                            it.tryOrNull{ value -> (value as com.typesafe.config.ConfigObject).toConfig() }?.let { slotConfig ->
                                 for (i in 0 until size) {
                                    if (content[i] == null)
                                        content[i] = InventorySlot(i).register(slotConfig)
                                }
                            }
                        }
                    }
                    if (gui.getBoolean("global"))
                        globalInventory = createInventory()
                }
                onRegister(this, cfg)
            }
        }
    }
}