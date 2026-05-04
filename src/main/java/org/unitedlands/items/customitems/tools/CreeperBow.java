package org.unitedlands.items.customitems.tools;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.items.util.PermissionsManager;

public class CreeperBow extends CustomTool implements Listener {

    private final Plugin plugin;
    private final PermissionsManager permissionsManager;
    private final NamespacedKey arrowKey;
    private final double explosiveChance;
    private final double velocityMultiplier;
    private final float explosionStrength;
    private final boolean breakBlocks;
    private Sound soundEffect = null;
    private Particle particleEffect = null;

    public CreeperBow(Plugin plugin) {
        this.plugin = plugin;
        this.permissionsManager = new PermissionsManager();
        this.arrowKey = new NamespacedKey(plugin, "creeper_arrow");

        // Construct the arrow settings from the config file.
        this.explosiveChance = plugin.getConfig().getDouble("items.creeper_bow.explosive-chance", 0.15);
        this.velocityMultiplier = plugin.getConfig().getDouble("items.creeper_bow.velocity-multiplier", 1.0);
        this.explosionStrength = (float) plugin.getConfig().getDouble("items.creeper_bow.explosion-strength", 4.0);
        this.breakBlocks = plugin.getConfig().getBoolean("items.creeper_bow.enable-explosion-damage", true);

        String soundString = plugin.getConfig().getString("items.creeper_bow.sound-effect");
        if (soundString != null && !soundString.isEmpty()) {
            NamespacedKey sKey = soundString.contains(":") ? NamespacedKey.fromString(soundString) : NamespacedKey.minecraft(soundString);
            if (sKey != null) {
                this.soundEffect = Registry.SOUNDS.get(sKey);
                if (this.soundEffect == null) plugin.getLogger().warning("Invalid sound-effect for creeper_bow in config.yml");
            }
        }

        String particleString = plugin.getConfig().getString("items.creeper_bow.particle-effect");
        if (particleString != null && !particleString.isEmpty()) {
            NamespacedKey pKey = particleString.contains(":") ? NamespacedKey.fromString(particleString) : NamespacedKey.minecraft(particleString);
            if (pKey != null) {
                this.particleEffect = Registry.PARTICLE_TYPE.get(pKey);
                if (this.particleEffect == null) plugin.getLogger().warning("Invalid particle-effect for creeper_bow in config.yml");
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void handleProjectileLaunch(Player player, ProjectileLaunchEvent event, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        if (Math.random() <= explosiveChance) {

            arrow.getPersistentDataContainer().set(arrowKey, PersistentDataType.BOOLEAN, true);

            if (velocityMultiplier != 1.0) {
                arrow.setVelocity(arrow.getVelocity().multiply(velocityMultiplier));
            }

            if (soundEffect != null) {
                arrow.getWorld().playSound(arrow.getLocation(), soundEffect, 1.0f, 1.0f);
            }

            if (particleEffect != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (arrow.isDead() || arrow.isInBlock() || arrow.isOnGround()) {
                            this.cancel();
                            return;
                        }
                        arrow.getWorld().spawnParticle(particleEffect, arrow.getLocation(), 2, 0, 0, 0, 0.05);
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
        }
    }

    @EventHandler
    public void onCreeperArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.getPersistentDataContainer().has(arrowKey, PersistentDataType.BOOLEAN)) {
            arrow.getWorld().createExplosion(arrow.getLocation(), explosionStrength, false, breakBlocks, arrow);
            arrow.remove();
        }
    }

    @EventHandler
    public void onCreeperArrowExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.getPersistentDataContainer().has(arrowKey, PersistentDataType.BOOLEAN)) {

            if (!breakBlocks) {
                event.blockList().clear();
                return;
            }

            if (arrow.getShooter() instanceof Player shooter) {
                event.blockList().removeIf(block -> !permissionsManager.canInteract(shooter, block));
            }
        }
    }
}