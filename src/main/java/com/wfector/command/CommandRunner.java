package com.wfector.command;

import java.util.UUID;
import java.util.logging.Level;

import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.wfector.notifier.BatchRunner;
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
            new Help(plugin).SendDialog(sender);
            return true;
        } else {
            if(args[0].equalsIgnoreCase("reload") && (sender.hasPermission("csn.command.reload"))) {
                plugin.updateConfiguration(sender);
                sender.sendMessage(plugin.getMessage("reload-cmd"));
                return true;

            } else if(args[0].equalsIgnoreCase("convert") && sender.hasPermission("csn.admin")) {

                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(plugin.getMessage("database-error"));
                    return true;
                }

                sender.sendMessage(plugin.getMessage("database-convert"));
                plugin.getLogger().log(Level.INFO, "Attempting to convert database...");

                new Converter(plugin, sender).runTaskAsynchronously(plugin);
                return true;

            } else if(args[0].equalsIgnoreCase("upload") && sender.hasPermission("csn.command.upload")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(plugin.getMessage("database-error"));
                    return true;
                }

                new BatchRunner(plugin).runTaskAsynchronously(plugin);

                sender.sendMessage(plugin.getMessage("database-upload"));

            } else if(args[0].equalsIgnoreCase("cleandatabase") && sender.hasPermission("csn.command.cleandatabase")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(plugin.getMessage("database-error"));
                    return true;
                }

                CleanDatabase cleaner = new CleanDatabase(plugin, sender);

                if (args.length > 1) {
                    for (int i = 1; i < args.length; i++) {
                        CleanDatabase.Parameter param = CleanDatabase.Parameter.getFromInput(args[i]);
                        if (param != null) {
                            if (i + 1 + param.getArgs().length > args.length) {
                                sender.sendMessage(plugin.getMessage("missing-arguments", "usage", param.getUsage()));
                                return true;
                            }
                            switch (param) {
                                case OLDER_THAN:
                                    try {
                                        int days = Integer.parseInt(args[i + 1]);
                                        cleaner.cleanBefore(days);
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(plugin.getMessage("invalid-number", "typo", args[i + 1]).replace("{usage}", param.getUsage()));
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
                                        sender.sendMessage(plugin.getMessage("invalid-username", "typo", args[i + 1]).replace("{usage}", param.getUsage())); //should work?
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
                new Help(plugin).SendDialog(sender);
                return true;

            } else if(args[0].equalsIgnoreCase("history") && sender.hasPermission("csn.command.history")) {

                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(plugin.getMessage("database-error"));
                    return true;
                }


                boolean markRead;
                UUID userId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : NameManager.getUUID(Properties.ADMIN_SHOP_NAME);
                int page = 1;
                if(args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e1) {
                        if(sender.hasPermission("csn.command.history.others")) {
                            userId = NameManager.getUUID(args[1]);
                            if (userId == null) {
                                OfflinePlayer target = plugin.getServer().getPlayer(args[1]);
                                if (target == null) {
                                    target = plugin.getServer().getOfflinePlayer(args[1]);
                                }
                                if (target != null) {
                                    userId = target.getUniqueId();
                                } else {
                                    sender.sendMessage(plugin.getMessage("user-not-found", "player", args[1]));
                                    return true;
                                }
                            }
                            if (args.length > 2) {
                                try {
                                    page = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e2) {
                                    sender.sendMessage(plugin.getMessage("page-not-found-other", "page", args[2]));
                                    return true;
                                }
                            }
                        } else {
                            sender.sendMessage(plugin.getMessage("page-not-found", "page", args[1]));
                            return true;
                        }
                    }

                    markRead = false;
                } else {
                    markRead = true;
                }

                new History(plugin, userId, sender, page, markRead).runTaskAsynchronously(plugin);

                return true;

            } else if(args[0].equalsIgnoreCase("clear") && sender.hasPermission("csn.command.clear")) {
                if(!plugin.isPluginEnabled()) {
                    sender.sendMessage(plugin.getMessage("database-error"));
                    return true;
                }

                new Clear(plugin, sender).runTaskAsynchronously(plugin);

                return true;
            }
        }

        sender.sendMessage(plugin.getMessage("unrecognized-command"));
        return true;
    }
}
