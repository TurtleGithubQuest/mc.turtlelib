package dev.turtle.turtlelib.gui

import com.typesafe.config.Config
import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.util.wrapper.CIMutableMap
import dev.turtle.turtlelib.util.configuration.ConfigUtils.getStringOrNull

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
                            (value as com.typesafe.config.ConfigObject).toConfig().let { slotConfig ->
                                (if (indexStr.lowercase() == "default") "-1" else indexStr).toIntOrNull()?.let { index ->
                                    content[index] = InventorySlot(index).register(slotConfig)
                                }?: m.newMessage("&dTurtleGUI&7: Invalid index '&e$indexStr&7' in &e$name&7.").send()
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