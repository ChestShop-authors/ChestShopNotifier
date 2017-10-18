package com.wfector.command;

import java.util.ArrayList;

import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Help {
    private static ChestShopNotifier plugin;

    public Help(ChestShopNotifier main) {
        plugin = main;
    }

    public static void SendDialog(CommandSender sender) {

        ArrayList<String> helpItems = new ArrayList<String>();
        if(plugin.getMessage("help.header") != null) {
            helpItems.add(plugin.getMessage("help.header"));
        }
        if(plugin.getMessage("help.help") != null) {
            helpItems.add(plugin.getMessage("help.help"));
        }
        if(sender.hasPermission("csn.command.history") && plugin.getMessage("help.history") != null) {
            helpItems.add(plugin.getMessage("help.history"));
        }
        if(sender.hasPermission("csn.command.read") && plugin.getMessage("help.read") != null) {
            helpItems.add(plugin.getMessage("help.read"));
        }
        if(sender.hasPermission("csn.command.clear") && plugin.getMessage("help.clear") != null) {
            helpItems.add(plugin.getMessage("help.clear"));
        }
        if(sender.hasPermission("csn.command.history.others") && plugin.getMessage("help.history-others") != null) {
            helpItems.add(plugin.getMessage("help.history-others"));
        }
        if(sender.hasPermission("csn.command.cleandatabase") && plugin.getMessage("help.cleandatabase") != null) {
            helpItems.add(plugin.getMessage("help.cleandatabase"));
        }
        if(sender.hasPermission("csn.command.cleandatabase") && plugin.getMessage("help.cleandatabase-older-than") != null) {
            helpItems.add(plugin.getMessage("help.cleandatabase-older-than"));
        }
        if(sender.hasPermission("csn.command.cleandatabase") && plugin.getMessage("help.cleandatabase-user") != null) {
            helpItems.add(plugin.getMessage("help.cleandatabase-user"));
        }
        if(sender.hasPermission("csn.command.cleandatabase") && plugin.getMessage("help.cleandatabase-read-only") != null) {
            helpItems.add(plugin.getMessage("help.cleandatabase-read-only"));
        }
        if(sender.hasPermission("csn.command.cleandatabase") && plugin.getMessage("help.cleandatabase-all") != null) {
            helpItems.add(plugin.getMessage("help.cleandatabase-all"));
        }
        if(sender.hasPermission("csn.command.upload") && plugin.getMessage("help.upload") != null) {
            helpItems.add(plugin.getMessage("help.upload"));
        }
        if(sender.hasPermission("csn.command.convert") && plugin.getMessage("help.convert") != null) {
            helpItems.add(plugin.getMessage("help.convert"));
        }
        if(sender.hasPermission("csn.command.reload") && plugin.getMessage("help.reload") != null) {
            helpItems.add(plugin.getMessage("help.reload"));
        }

        for(String item : helpItems) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', item));
        }

    }

}
