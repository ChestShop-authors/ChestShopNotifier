package com.wfector.command;

import java.util.UUID;
import java.util.logging.Level;

import com.wfector.notifier.BatchRunner;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wfector.notifier.ChestShopNotifier;

public class CommandRunner implements CommandExecutor {
    private final ChestShopNotifier plugin;

    public CommandRunner(ChestShopNotifier plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length == 0) {
            Help.SendDialog(sender);
        } else {
            if(args[0].equalsIgnoreCase("reload") && (sender.hasPermission("csn.admin"))) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GRAY + "Reloading, please wait...");

                plugin.updateConfiguration(sender);

            } else if(args[0].equalsIgnoreCase("convert") && sender.hasPermission("csn.admin")) {

                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                sender.sendMessage(ChatColor.RED + "Attempting to convert database...");
                plugin.getLogger().log(Level.INFO, "Attempting to convert database...");

                new Converter(plugin, sender).runTaskAsynchronously(plugin);

            } else if(args[0].equalsIgnoreCase("upload") && sender.hasPermission("csn.admin")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                new BatchRunner(plugin).runTaskAsynchronously(plugin);

                sender.sendMessage(ChatColor.RED + "Batch executed!");

            } else if(args[0].equalsIgnoreCase("help") && sender.hasPermission("csn.user")) {
                Help.SendDialog(sender);

            } else  if(args[0].equalsIgnoreCase("history") && sender.hasPermission("csn.user")) {
                OfflinePlayer target;

                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                if(args.length > 1) {
                    if(args.length > 2) {
                        sender.sendMessage(ChatColor.RED + "Too many arguments! /csn history [username]");
                        return true;
                    }

                    if(sender.hasPermission("csn.history.others") || sender.hasPermission("csn.admin")) {
                        target = plugin.getServer().getPlayer(args[1]);
                        if (target == null) {
                            plugin.getServer().getOfflinePlayer(args[1]);
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have the permission to see other user's history! You can only use /csn history!");
                        return true;
                    }

                    if(target == null) {
                        sender.sendMessage(ChatColor.RED + "The user '" + args[1] + "' was not found.");
                        return true;
                    }
                } else {
                    if(!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "The console has no sales!");
                        return true;
                    }
                    target = (Player) sender;
                }

                final UUID userId = target.getUniqueId();

                new History(plugin, userId, sender).runTaskAsynchronously(plugin);

                return true;

            } else if(args[0].equalsIgnoreCase("clear") && sender.hasPermission("csn.user")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                UUID senderId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");

                new Clear(plugin, senderId).runTaskAsynchronously(plugin);
                if(plugin.getMessage("history-clear") != null) sender.sendMessage(plugin.getMessage("history-clear"));

                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Command not recognized. Type /csn help for help.");
        return true;
    }
}
