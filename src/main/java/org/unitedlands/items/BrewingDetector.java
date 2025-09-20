package org.unitedlands.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.items.util.BrewingRecipe;
import org.unitedlands.items.util.BrewingRecipeItem;

import dev.lone.itemsadder.api.CustomStack;

public class BrewingDetector implements Listener {

    private final UnitedItems plugin;

    private final Map<BrewerInventory, BukkitRunnable> activeBrews = new HashMap<>();
    private Set<BrewingRecipe> recipes = new HashSet<>();

    public BrewingDetector(UnitedItems plugin) {
        this.plugin = plugin;

        loadRecipes();
    }

    public void loadRecipes() {
        var recipeSection = plugin.getConfig().getConfigurationSection("brewing_recipes");
        for (var key : recipeSection.getKeys(false)) {
            var s = recipeSection.getConfigurationSection(key);

            var permission = s.getString("permission");

            var ingredientItem = s.getString("ingredient.item");
            var ingredientType = s.getString("ingredient.type");

            var baseItem = s.getString("base.item");
            var baseType = s.getString("base.type");

            var resultItem = s.getString("result.item");
            var resultType = s.getString("result.type");

            recipes.add(new BrewingRecipe(permission, new BrewingRecipeItem(ingredientItem, ingredientType),
                    new BrewingRecipeItem(baseItem, baseType), new BrewingRecipeItem(resultItem, resultType)));

        }
        plugin.getLogger().info("Loaded " + recipes.size() + " brewing recipes.");
    }

    /**
     * Handle putting custom items into the ingredient slot.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

        if (!(e.getInventory() instanceof BrewerInventory inv))
            return;

        if (e.getRawSlot() == 3) { // slot 3 = ingredient

            // Don't do anything if these is no base potion
            if (inv.getItem(0) == null && inv.getItem(1) == null && inv.getItem(2) == null)
                return;

            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {

                // If the brewing stand is already brewing, don't allow switching of the
                // ingredient.
                if (activeBrews.containsKey(inv)) {
                    e.setCancelled(true);
                    return;
                }

                // See if the ingredient is in any of the recipies so that we can trigger the
                // custom brewing process.

                var matchingRecipes = getMatchingRecipes((Player) e.getWhoClicked(), cursor);
                if (matchingRecipes.size() == 0) {
                    return;
                }

                // At least one recipe uses the ingredient. Force it into the slot and start
                // the brewing process.

                inv.setIngredient(cursor.clone());
                cursor.setAmount(0);
                e.setCancelled(true);

                // Start brewing
                startBrewing(inv, (Player) e.getWhoClicked(), matchingRecipes);

            } else {
                setBrewTimeOnBlock(inv, 0);
                stopBrewingTask(inv);
            }

        } else if (e.getRawSlot() >= 0 && e.getRawSlot() < 3) {

            if (activeBrews.containsKey(inv)) {
                e.setCancelled(true);
                return;
            }

        }
    }

    private List<BrewingRecipe> getMatchingRecipes(Player player, ItemStack item) {

        String itemId = "";

        // Determine if the item is a custom item
        CustomStack customItem = CustomStack.byItemStack(item);
        if (customItem != null) {
            itemId = customItem.getNamespacedID();
        } else {
            itemId = item.getType().toString();
        }

        List<BrewingRecipe> matchingRecipes = new ArrayList<>();
        String finalItemId = itemId;
        for (BrewingRecipe recipe : recipes) {

            if (recipe.getPermission() != null) {
                if (!player.hasPermission(recipe.getPermission()))
                    continue;
            }

            String ingredientItem = recipe.getIngredient().getItem();
            String ingredientType = recipe.getIngredient().getType();

            if (ingredientItem.equals(finalItemId)) {
                if (ingredientType != null && !ingredientType.isBlank()) {
                    if (item.getItemMeta() instanceof PotionMeta potionMeta) {
                        if (potionMeta.getBasePotionType().toString().equals(ingredientType))
                            matchingRecipes.add(recipe);
                    }
                } else {
                    matchingRecipes.add(recipe);
                }
            }
        }

        return matchingRecipes;
    }

    private void startBrewing(BrewerInventory inv, Player player, List<BrewingRecipe> matchingRecipes) {
        if (activeBrews.containsKey(inv))
            return; // Already brewing

        BrewingStand stand = inv.getHolder();
        if (stand.getFuelLevel() <= 0) {
            player.sendMessage("§cThis brewing stand is out of fuel!");
            return; // don’t start
        }
        stand.setFuelLevel(stand.getFuelLevel() - 1); // spend 1 fuel
        stand.update(true, false);

        plugin.getLogger().info("Brewing start...");
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= 400) { // 20 seconds
                    stopBrewingTask(inv);
                    finishBrewing(player, inv, matchingRecipes);
                    cancel();
                } else {
                    ticks++;

                    // Update progress bar
                    setBrewTimeOnBlock(inv, 400 - ticks);

                    // Every second, play brewing sound + particles
                    if (ticks % 20 == 0) {
                        if (inv.getHolder() != null && inv.getHolder().getWorld() != null) {
                            inv.getHolder().getWorld().playSound(inv.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f,
                                    1f);
                            inv.getHolder().getWorld().spawnParticle(Particle.SMOKE,
                                    inv.getLocation().add(0.5, 1.0, 0.5),
                                    5, 0.2, 0.2, 0.2, 0.01);
                        }
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 1L, 1L);
        activeBrews.put(inv, task);
    }

    /**
     * Replace potions with custom effects.
     */
    private void finishBrewing(Player player, BrewerInventory inv, List<BrewingRecipe> matchingRecipes) {

        for (int i = 0; i < 3; i++) {

            var baseItem = inv.getItem(i);
            if (baseItem == null)
                continue;

            // Do we have a recipe that requires this base item?
            BrewingRecipe matchingRecipe = getBaseMatchingRecipe(matchingRecipes, baseItem);

            if (matchingRecipe != null) {

                String resultItem = matchingRecipe.getResult().getItem();
                String resultType = matchingRecipe.getResult().getType();

                // Replace the output with the desired custom result

                ItemStack result = null;
                CustomStack customResult = CustomStack.getInstance(resultItem);
                if (customResult != null) {
                    result = customResult.getItemStack();
                } else {
                    result = new ItemStack(Material.valueOf(resultItem));
                }

                if (result == null)
                    return;

                // See if the result is the special case of combining two source potions
                if (resultType != null && resultType.equals("COMBINE")) {

                    // Combining only works when the result is also a potion
                    if (result.getItemMeta() instanceof PotionMeta resultMeta) {

                        // Apply all effects of the base item
                        if (baseItem.getItemMeta() instanceof PotionMeta baseItemMeta) {
                            for (PotionEffect effect : baseItemMeta.getAllEffects()) {
                                resultMeta.addCustomEffect(effect, true);
                            }
                        }

                        // Apply all effects of the ingredients
                        var ingredient = inv.getIngredient();
                        if (ingredient.getItemMeta() instanceof PotionMeta ingredientMeta) {
                            for (PotionEffect effect : ingredientMeta.getAllEffects()) {
                                resultMeta.addCustomEffect(effect, true);
                            }
                        }

                        resultMeta.setCustomPotionName("Blended Potion");
                        result.setItemMeta(resultMeta);
                    }
                } else {
                    if (resultType != null && !resultType.isBlank()) {
                        if (result.getItemMeta() instanceof PotionMeta potionMeta) {
                            try {
                                var basePotionType = PotionType.valueOf(resultType);
                                potionMeta.setBasePotionType(basePotionType);
                                result.setItemMeta(potionMeta);
                            } catch (Exception ex) {
                                plugin.getLogger().severe("Could not fine potion type " + resultType);
                            }
                        }
                    }
                }

                inv.setItem(i, result);

            } else {

                var ruinedPotion = new ItemStack(Material.POTION);
                if (ruinedPotion.getItemMeta() instanceof PotionMeta meta) {
                    meta.setBasePotionType(PotionType.AWKWARD);
                    meta.setColor(Color.BLACK);
                    ruinedPotion.setItemMeta(meta);
                }

                inv.setItem(i, ruinedPotion);
            }

        }

        // Play effect

        if (inv.getHolder() != null && inv.getHolder().getWorld() != null) {
            inv.getHolder().getWorld().playSound(inv.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            inv.getHolder().getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    inv.getLocation().add(0.5, 1.0, 0.5),
                    20, 0.3, 0.3, 0.3, 0.1);
        }

        // Consume one ingredient

        ItemStack ing = inv.getIngredient();
        if (ing != null) {
            ing.setAmount(ing.getAmount() - 1);

            if (ing.getAmount() > 0) {
                inv.setIngredient(ing);
            } else {
                inv.setIngredient(null);
            }
        }

    }

    private BrewingRecipe getBaseMatchingRecipe(List<BrewingRecipe> matchingRecipes, ItemStack item) {
        String baseItemId = "";

        // Determine if the base item is a custom item
        CustomStack customBaseItem = CustomStack.byItemStack(item);
        if (customBaseItem != null) {
            baseItemId = customBaseItem.getNamespacedID();
        } else {
            baseItemId = item.getType().toString();
        }

        BrewingRecipe matchingRecipe = null;
        for (BrewingRecipe recipe : matchingRecipes) {

            String baseItem = recipe.getBase().getItem();
            String baseType = recipe.getBase().getType();

            if (baseItem.equals(baseItemId)) {
                if (baseType != null && !baseType.isBlank()) {
                    if (item.getItemMeta() instanceof PotionMeta potionMeta) {
                        if (potionMeta.getBasePotionType().toString().equals(baseType))
                            matchingRecipe = recipe;
                    }
                } else {
                    matchingRecipe = recipe;
                }
            }
        }
        return matchingRecipe;
    }

    /**
     * Cancel brewing task.
     */
    private void stopBrewingTask(BrewerInventory inv) {
        BukkitRunnable task = activeBrews.remove(inv);
        if (task != null) {
            task.cancel();
        }
    }

    /*
     * Update the brewing time display
     */
    private void setBrewTimeOnBlock(BrewerInventory inv, int timeLeft) {
        BrewingStand stand = inv.getHolder();
        timeLeft = Math.max(0, Math.min(400, timeLeft));
        stand.setBrewingTime(timeLeft);
        stand.update(true, false);
    }

}
