package com.wfector.command;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Help {

    public static void SendDialog(CommandSender sender) {

        ArrayList<String> helpItems = new ArrayList<String>();

        helpItems.add("&dChestShop Notifier // &7Commands");
        helpItems.add("&c ");
        helpItems.add("&7- /csn &dhelp &f- Plugin usage & commands");
        helpItems.add("&7- /csn &dhistory &f- View unread sales");
        helpItems.add("&7- /csn &dclear &f- Mark all sales as read");

        if(sender.isOp() || sender.hasPermission("csn.admin")) {
            helpItems.add("&c ");
            helpItems.add("&7- /csn &dupload &f- Force update databases");
            helpItems.add("&7- /csn &dconvert &f- Convert database to UUIDs");
            helpItems.add("&7- /csn &dreload &f- Reload configuration");
        }

        for(String item : helpItems) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', item));
        }

    }

}
