package com.wfector.command;

import java.util.ArrayList;

import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.command.CommandSender;

public class Help {
    private final ChestShopNotifier plugin;

    public Help(ChestShopNotifier main) {
        plugin = main;
    }

    public void SendDialog(CommandSender sender) {

        ArrayList<String> helpItems = new ArrayList<String>();
        helpItems.add(plugin.getMessage("help.header"));
        helpItems.add(plugin.getMessage("help.help"));
        if(sender.hasPermission("csn.command.history")) {
            helpItems.add(plugin.getMessage("help.history"));
        }
        if(sender.hasPermission("csn.command.read")) {
            helpItems.add(plugin.getMessage("help.read"));
        }
        if(sender.hasPermission("csn.command.clear")) {
            helpItems.add(plugin.getMessage("help.clear"));
        }
        if(sender.hasPermission("csn.command.history.others")) {
            helpItems.add(plugin.getMessage("help.history-others"));
        }
        if(sender.hasPermission("csn.command.cleandatabase")) {
            helpItems.add(plugin.getMessage("help.cleandatabase"));
            helpItems.add(plugin.getMessage("help.cleandatabase-older-than"));
            helpItems.add(plugin.getMessage("help.cleandatabase-user"));
            helpItems.add(plugin.getMessage("help.cleandatabase-read-only"));
            helpItems.add(plugin.getMessage("help.cleandatabase-all"));
        }
        if(sender.hasPermission("csn.command.upload")) {
            helpItems.add(plugin.getMessage("help.upload"));
        }
        if(sender.hasPermission("csn.command.convert")) {
            helpItems.add(plugin.getMessage("help.convert"));
        }
        if(sender.hasPermission("csn.command.reload")) {
            helpItems.add(plugin.getMessage("help.reload"));
        }

        for(String item : helpItems) {
            sender.sendMessage(item);
        }
    }
}
