package org.unitedlands.items;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.items.commands.UnitedItemsCommands;
import org.unitedlands.items.commands.UpdateItemCommand;

public class UnitedItems extends JavaPlugin {

    private ItemDetector itemDetector;
    private BrewingDetector brewingDetector;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        itemDetector = new ItemDetector(this);
        getServer().getPluginManager().registerEvents(itemDetector, this);
        brewingDetector = new BrewingDetector(this);
        getServer().getPluginManager().registerEvents(brewingDetector, this);

        getCommand("uniteditems").setExecutor(new UnitedItemsCommands(this));
        getCommand("updateitem").setExecutor(new UpdateItemCommand(this));
    }

    @Override
    public void onDisable() {
        if (itemDetector != null) {
            itemDetector.saveData();
        }
    }

    public ItemDetector getItemDetector() {
        return itemDetector;
    }

    public BrewingDetector getBrewingDetector() {
        return brewingDetector;
    }

}
