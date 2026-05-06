package org.unitedlands.items.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.utils.Logger;

public class CustomRecipeManager implements Listener {

    private final UnitedItems plugin;

    private HashSet<String> recipeIds = new HashSet<>();

    public CustomRecipeManager(UnitedItems plugin) {
        this.plugin = plugin;
        loadRecipes();
    }

    public void loadRecipes() {
        recipeIds = new HashSet<>();
        var config = plugin.getRecipeConfig().get();
        for (String key : config.getKeys(false)) {
            recipeIds.add(key);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {

        if (event.getRecipe() instanceof CraftingRecipe craftingRecipe) {

            var id = craftingRecipe.getKey().asString();
            if (!recipeIds.contains(id))
                return;

            if (event.getWhoClicked() instanceof Player player) {

                var itemFactory = UnitedLib.getInstance().getItemFactory();

                ConfigurationSection returnItemSection = plugin.getRecipeConfig().get()
                        .getConfigurationSection(id + ".return-items");
                if (returnItemSection != null) {

                    HashMap<String, Integer> returnItems = new HashMap<>();
                    for (var itemSectionKey : returnItemSection.getKeys(false)) {
                        var itemId = returnItemSection.getString(itemSectionKey + ".item");
                        if (itemId != null && !itemId.isEmpty())
                            returnItems.put(itemId, returnItemSection.getInt(itemSectionKey + ".damage", 0));
                    }

                    var inv = event.getInventory();
                    Map<Integer, ItemStack> itemsToReturn = new HashMap<>();

                    for (var i = 0; i < inv.getSize(); i++) {
                        var item = inv.getItem(i);
                        if (item != null) {

                            var invItemId = itemFactory.getId(item);
                            if (returnItems.containsKey(invItemId)) {

                                var damage = returnItems.get(invItemId);
                                if (damage != 0 && item.getItemMeta() instanceof Damageable damageable) {

                                    var currentDamage = 0;
                                    if (damageable.hasDamageValue()) {
                                        currentDamage = damageable.getDamage();
                                    }

                                    var newDamage = currentDamage + damage;
                                    damageable.setDamage(newDamage);
                                    item.setItemMeta(damageable);
                                }

                                itemsToReturn.put(i, item.clone());

                            }
                        }
                    }

                    for (var entrySet : itemsToReturn.entrySet()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            inv.setItem(entrySet.getKey(), entrySet.getValue());
                        }, 1);
                    }

                }

                ConfigurationSection additionalItemSection = plugin.getRecipeConfig().get()
                        .getConfigurationSection(id + ".additional-items");
                if (additionalItemSection != null) {

                    HashMap<String, Integer> additionaltems = new HashMap<>();
                    for (var itemSectionKey : additionalItemSection.getKeys(false)) {
                        var itemId = additionalItemSection.getString(itemSectionKey + ".item");
                        if (itemId != null && !itemId.isEmpty())
                            additionaltems.put(itemId, additionalItemSection.getInt(itemSectionKey + ".amount", 0));
                    }

                    if (additionaltems.size() > 0) {
                        for (var additionalItem : additionaltems.entrySet()) {
                            var amount = additionalItem.getValue();
                            if (amount > 0) {
                                var item = itemFactory.getItemStack(additionalItem.getKey(), amount);
                                if (item != null) {
                                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                                    for (var leftoverItem : leftover.entrySet())
                                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(),
                                                leftoverItem.getValue());

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
