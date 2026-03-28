package com.enhancedjobs.commands;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.gui.JobsAdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsAdminCommand implements CommandExecutor {

    private final EnhancedJobSystem plugin;

    public JobsAdminCommand(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enhancedjobs.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use /jobsadmin.");
            return true;
        }
        JobsAdminGUI.open(plugin, player);
        return true;
    }
}
