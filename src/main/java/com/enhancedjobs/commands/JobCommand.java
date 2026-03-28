package com.enhancedjobs.commands;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.gui.JobInfoGUI;
import com.enhancedjobs.gui.JobSelectionGUI;
import com.enhancedjobs.gui.QuestGUI;
import com.enhancedjobs.jobs.JobType;
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
 * /job [info [jobname] | quests [jobname] | leave <jobname>]
 *
 * With no arguments, opens the Job Selection GUI.
 */
public class JobCommand implements CommandExecutor, TabCompleter {

    private final EnhancedJobSystem plugin;

    public JobCommand(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        if (!player.hasPermission("enhancedjobs.use")) {
            player.sendMessage("§c[Jobs] You don't have permission to use this.");
            return true;
        }

        if (args.length == 0) {
            new JobSelectionGUI(plugin).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "info" -> {
                if (args.length < 2) {
                    new JobSelectionGUI(plugin).open(player);
                    return true;
                }
                JobType job = JobType.fromString(args[1]);
                if (job == null) { player.sendMessage("§c[Jobs] Unknown job: " + args[1]); return true; }
                new JobInfoGUI(plugin, job).open(player);
            }

            case "quests" -> {
                if (args.length < 2) {
                    // Open quest GUI for first active job, or selection GUI
                    var data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                    if (data.getActiveJobs().isEmpty()) {
                        new JobSelectionGUI(plugin).open(player);
                    } else {
                        String first = data.getActiveJobs().iterator().next();
                        JobType job = JobType.fromString(first);
                        if (job != null) new QuestGUI(plugin, job).open(player);
                    }
                    return true;
                }
                JobType job = JobType.fromString(args[1]);
                if (job == null) { player.sendMessage("§c[Jobs] Unknown job: " + args[1]); return true; }
                new QuestGUI(plugin, job).open(player);
            }

            case "leave" -> {
                if (args.length < 2) {
                    player.sendMessage("§c[Jobs] Usage: /job leave <jobname>");
                    return true;
                }
                JobType job = JobType.fromString(args[1]);
                if (job == null) { player.sendMessage("§c[Jobs] Unknown job: " + args[1]); return true; }
                boolean left = plugin.getJobManager().leaveJob(player, job);
                if (!left) player.sendMessage("§c[Jobs] You don't have the " + job.getDisplayName() + " job.");
            }

            default -> {
                player.sendMessage("§e[Jobs] Usage: /job | /job info [job] | /job quests [job] | /job leave <job>");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("info", "quests", "leave")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.stream(JobType.values())
                    .map(j -> j.name().toLowerCase())
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
