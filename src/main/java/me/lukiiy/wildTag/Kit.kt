package me.lukiiy.wildTag

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.function.Consumer

class Kit(vararg items: ItemStack) {
    private val items: MutableList<ItemStack> = mutableListOf()

    init {
        items.forEach {
            it.apply {
                setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                setData(DataComponentTypes.UNBREAKABLE)
                addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
                editPersistentDataContainer { d -> d.set(KEY, PersistentDataType.INTEGER, 1) }
            }

            this.items.add(it)
        }
    }

    fun apply(inventory: PlayerInventory) = items.forEach(Consumer { inventory.addItem(it!!.clone()) })

    fun get(): MutableList<ItemStack?> = Collections.unmodifiableList(items)

    companion object {
        val KEY: NamespacedKey = NamespacedKey(WildTag.getInstance(), "k")

        @JvmStatic
        fun isKitItem(item: ItemStack): Boolean = item.persistentDataContainer.has(KEY, PersistentDataType.INTEGER)
    }
}
