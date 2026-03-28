package com.enhancedjobs.commands;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.jobs.JobType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * /jobadmin <subcommand> [args]
 *
 * Subcommands:
 *   setlevel  <player> <job> <level>    – set a player's job level
 *   addxp     <player> <job> <amount>   – add XP to a player's job
 *   resetquests <player>                – force-reset a player's daily quests
 *   setvendor <npcId>                   – register/unregister a ZNPCS NPC as a crop vendor
 *   reload                              – reload config.yml
 */
public class JobAdminCommand implements CommandExecutor, TabCompleter {

    private final EnhancedJobSystem plugin;

    public JobAdminCommand(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!sender.hasPermission("enhancedjobs.admin")) {
            sender.sendMessage("§c[Jobs] No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /jobadmin setlevel <player> <job> <level>
            case "setlevel" -> {
                if (args.length < 4) { sender.sendMessage("§cUsage: /jobadmin setlevel <player> <job> <level>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found or offline."); return true; }
                JobType job = JobType.fromString(args[2]);
                if (job == null) { sender.sendMessage("§cUnknown job: " + args[2]); return true; }
                int level;
                try { level = Integer.parseInt(args[3]); } catch (NumberFormatException e) { sender.sendMessage("§cInvalid level."); return true; }
                plugin.getJobManager().adminSetLevel(target, job, level);
                sender.sendMessage("§a[Jobs] Set " + target.getName() + "'s " + job.getDisplayName() + " level to " + level + ".");
                target.sendMessage("§6[Jobs] An admin set your " + job.getDisplayName() + " level to " + level + ".");
            }

            // /jobadmin addxp <player> <job> <amount>
            case "addxp" -> {
                if (args.length < 4) { sender.sendMessage("§cUsage: /jobadmin addxp <player> <job> <amount>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found or offline."); return true; }
                JobType job = JobType.fromString(args[2]);
                if (job == null) { sender.sendMessage("§cUnknown job: " + args[2]); return true; }
                double xp;
                try { xp = Double.parseDouble(args[3]); } catch (NumberFormatException e) { sender.sendMessage("§cInvalid XP amount."); return true; }
                plugin.getJobManager().adminAddXP(target, job, xp);
                sender.sendMessage("§a[Jobs] Added " + xp + " XP to " + target.getName() + "'s " + job.getDisplayName() + " job.");
            }

            // /jobadmin resetquests <player>
            case "resetquests" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /jobadmin resetquests <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found or offline."); return true; }
                plugin.getJobManager().adminResetQuests(target);
                sender.sendMessage("§a[Jobs] Reset daily quests for " + target.getName() + ".");
                target.sendMessage("§6[Jobs] An admin reset your daily quests!");
            }

            // /jobadmin setvendor <npcId>
            case "setvendor" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /jobadmin setvendor <npcId>"); return true; }
                int npcId;
                try { npcId = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage("§cInvalid NPC ID."); return true; }
                var vendorMgr = plugin.getVendorManager();
                if (vendorMgr.isVendor(npcId)) {
                    vendorMgr.removeVendor(npcId);
                    sender.sendMessage("§e[Jobs] NPC #" + npcId + " is no longer a Crop Vendor.");
                } else {
                    vendorMgr.addVendor(npcId);
                    sender.sendMessage("§a[Jobs] NPC #" + npcId + " is now a Crop Vendor.");
                }
            }

            // /jobadmin reload
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§a[Jobs] Config reloaded.");
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l[JobAdmin] Commands:");
        sender.sendMessage("§e/jobadmin setlevel <player> <job> <level>");
        sender.sendMessage("§e/jobadmin addxp <player> <job> <amount>");
        sender.sendMessage("§e/jobadmin resetquests <player>");
        sender.sendMessage("§e/jobadmin setvendor <npcId>  §7(toggles vendor status)");
        sender.sendMessage("§e/jobadmin reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (!sender.hasPermission("enhancedjobs.admin")) return List.of();
        if (args.length == 1) {
            return Stream.of("setlevel", "addxp", "resetquests", "setvendor", "reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("setvendor") && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp"))) {
            return Arrays.stream(JobType.values())
                    .map(j -> j.name().toLowerCase())
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
