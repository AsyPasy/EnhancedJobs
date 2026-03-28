package com.enhancedjobs.commands;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.gui.MainJobsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsCommand implements CommandExecutor {

    private final EnhancedJobSystem plugin;

    public JobsCommand(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        MainJobsGUI.open(plugin, player);
        return true;
    }
}
