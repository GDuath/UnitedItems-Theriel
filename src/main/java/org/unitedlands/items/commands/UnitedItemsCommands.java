package org.unitedlands.items.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.items.UnitedItems;

public class UnitedItemsCommands implements CommandExecutor {

    private final UnitedItems plugin;

    public UnitedItemsCommands(UnitedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length == 0)
            return false;

        if (!sender.hasPermission("united.items.admin"))
            return false;

        if (args[0].equalsIgnoreCase("reload"))
        {
            plugin.reloadConfig();
            plugin.getBrewingDetector().loadRecipes();
            sender.sendMessage("UnitedItems config reloaded");
        }

        return true;
    }

}
