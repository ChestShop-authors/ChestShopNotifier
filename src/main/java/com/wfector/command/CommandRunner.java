package com.wfector.command;

import java.util.UUID;
import java.util.logging.Level;

import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.UUIDs.NameManager;
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
            if(args[0].equalsIgnoreCase("reload") && (sender.hasPermission("csn.command.reload"))) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GRAY + "Reloading, please wait...");

                plugin.updateConfiguration(sender);
                return true;

            } else if(args[0].equalsIgnoreCase("convert") && sender.hasPermission("csn.admin")) {

                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                sender.sendMessage(ChatColor.RED + "Attempting to convert database...");
                plugin.getLogger().log(Level.INFO, "Attempting to convert database...");

                new Converter(plugin, sender).runTaskAsynchronously(plugin);
                return true;

            } else if(args[0].equalsIgnoreCase("upload") && sender.hasPermission("csn.command.upload")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                new BatchRunner(plugin).runTaskAsynchronously(plugin);

                sender.sendMessage(ChatColor.RED + "Batch executed!");

            } else if(args[0].equalsIgnoreCase("cleandatabase") && sender.hasPermission("csn.command.cleandatabase")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                CleanDatabase cleaner = new CleanDatabase(plugin, sender);

                if (args.length > 1) {
                    for (int i = 1; i < args.length; i++) {
                        CleanDatabase.Parameter param = CleanDatabase.Parameter.getFromInput(args[i]);
                        if (param != null) {
                            if (i + 1 + param.getArgs().length > args.length) {
                                sender.sendMessage(ChatColor.RED + "Missing parameter arguments: " + param.getUsage());
                                return true;
                            }
                            switch (param) {
                                case OLDER_THAN:
                                    try {
                                        int days = Integer.parseInt(args[i + 1]);
                                        cleaner.cleanBefore(days);
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(ChatColor.RED + args[i + 1] + " is not a valid number input for " + param.getUsage() + "!");
                                        return true;
                                    }
                                    break;
                                case USER:
                                    UUID userId;
                                    try {
                                        userId = UUID.fromString(args[i+1]);
                                    } catch (IllegalArgumentException e) {
                                        userId = NameManager.getUUID(args[i+1]);
                                    }
                                    if (userId != null) {
                                        cleaner.cleanUser(userId);
                                    } else {
                                        sender.sendMessage(ChatColor.RED + args[i + 1] + " is not a valid username/uuid input for " + param.getUsage() + "!");
                                    }
                                    break;
                                case READ_ONLY:
                                    cleaner.cleanReadOnly(true);
                                    break;
                                case ALL:
                                    cleaner.cleanReadOnly(false);
                                    break;
                            }
                            i += param.getArgs().length;
                        }
                    }
                }

                cleaner.runTaskAsynchronously(plugin);

                return true;

            } else if(args[0].equalsIgnoreCase("help") && sender.hasPermission("csn.command")) {
                Help.SendDialog(sender);
                return true;

            } else if(args[0].equalsIgnoreCase("history") && sender.hasPermission("csn.command.history")) {

                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }


                boolean markRead;
                UUID userId;
                if(args.length > 1) {
                    if(args.length > 2) {
                        sender.sendMessage(ChatColor.RED + "Too many arguments! /csn history [username]");
                        return true;
                    }

                    OfflinePlayer target;
                    if(sender.hasPermission("csn.command.history.others")) {
                        target = plugin.getServer().getPlayer(args[1]);
                        if (target == null) {
                            target = plugin.getServer().getOfflinePlayer(args[1]);
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have the permission to see other user's history! You can only use /csn history!");
                        return true;
                    }

                    if(target == null) {
                        sender.sendMessage(ChatColor.RED + "The user '" + args[1] + "' was not found.");
                        return true;
                    }
                    userId = target.getUniqueId();
                    markRead = false;
                } else {
                    userId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : NameManager.getUUID(Properties.ADMIN_SHOP_NAME);
                    markRead = true;
                }

                new History(plugin, userId, sender, markRead).runTaskAsynchronously(plugin);

                return true;

            } else if(args[0].equalsIgnoreCase("clear") && sender.hasPermission("csn.command.clear")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
                    return true;
                }

                new Clear(plugin, sender).runTaskAsynchronously(plugin);

                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Command not recognized. Type /csn help for help.");
        return true;
    }
}
