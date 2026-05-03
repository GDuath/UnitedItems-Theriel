package org.unitedlands.items.customitems.tools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.util.PermissionsManager;

public class LuminousWand extends CustomTool implements Listener {

    private final PermissionsManager permissionsManager;

    public LuminousWand(Plugin plugin, PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void handleInteract(Player player, PlayerInteractEvent event, EquipmentSlot hand) {

        if (event.getHand() == null)
            return;

        if (!event.getHand().equals(EquipmentSlot.HAND))
            return;

        if (event.getClickedBlock() == null)
            return;

        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            boolean replaced = false;

            // Radius x:1, y:3, z:1
            for (int x = -1; x <= 1; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block b = clickedBlock.getRelative(x, y, z);
                        if (b.getType() == Material.AIR) {
                            if (permissionsManager.canInteract(player, b)) {
                                b.setType(Material.LIGHT);
                                replaced = true;
                            }
                        }
                    }
                }
            }

            if (replaced) {
                damageItem(event.getItem());
            }

        } else if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            // Radius x:10, y:10, z:10
            for (int x = -10; x <= 10; x++) {
                for (int y = -10; y <= 10; y++) {
                    for (int z = -10; z <= 10; z++) {
                        Block b = clickedBlock.getRelative(x, y, z);
                        if (b.getType() == Material.LIGHT) {
                            if (permissionsManager.canInteract(player, b)) {
                                b.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    private void damageItem(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(damageable.getDamage() + 1);
                item.setItemMeta(meta);
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);

        if (first == null || second == null) return;

        if (!UnitedLib.getInstance().getItemFactory().isCustomItem(first)) return;
        if (!UnitedLib.getInstance().getItemFactory().getId(first).contains("luminous_wand")) return;

        if (second.getType() == Material.GLOWSTONE || second.getType() == Material.GLOWSTONE_DUST) {

            ItemMeta meta = first.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int currentDamage = damageable.getDamage();

                if (currentDamage <= 0) return;

                ItemStack result = first.clone();
                Damageable resultMeta = (Damageable) result.getItemMeta();

                int repairAmount = Math.max(1, first.getType().getMaxDurability() / 4);
                int newDamage = Math.max(0, currentDamage - (repairAmount * second.getAmount()));

                resultMeta.setDamage(newDamage);
                result.setItemMeta(resultMeta);

                event.setResult(result);
                event.getView().setRepairCost(1);
            }
        }
    }
}
