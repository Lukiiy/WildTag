package me.lukiiy.wildTag

import org.bukkit.Material
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
            it.itemMeta = it.itemMeta.apply {
                persistentDataContainer.set(KEY, PersistentDataType.INTEGER, 1)
                if (it.type == Material.COMPASS) setEnchantmentGlintOverride(false)
                addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_ATTRIBUTES)
                isUnbreakable = true
            }

            this.items.add(it)
        }
    }

    fun apply(inventory: PlayerInventory) = items.forEach(Consumer { inventory.addItem(it!!.clone()) })

    fun get(): MutableList<ItemStack?> = Collections.unmodifiableList(items)

    companion object {
        val KEY: NamespacedKey = NamespacedKey(WildTag.getInstance(), "k")

        @JvmStatic
        fun isKitItem(item: ItemStack): Boolean {
            val m = item.itemMeta
            if (m == null) return false

            return m.persistentDataContainer.has(KEY, PersistentDataType.INTEGER)
        }
    }
}
