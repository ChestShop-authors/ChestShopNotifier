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
        if (sender.hasPermission("csn.command.history")) {
            helpItems.add("&7- /csn &dhistory &f- View unread sales");
        }
        if (sender.hasPermission("csn.command.read")) {
            helpItems.add("&7- /csn &dread &f- Mark all sales as read");
        }
        if (sender.hasPermission("csn.command.clear")) {
            helpItems.add("&7- /csn &dclear &f- Remove read sales");
        }
        helpItems.add("&c ");

        if (sender.hasPermission("csn.command.cleandatabase")) {
            helpItems.add("&7- /csn &cdcleandatabase &f- Remove database entries");
            helpItems.add("&7 Parameters:");
            helpItems.add("&c --older-than, -o <days> &f- Removes entries older than <days>");
            helpItems.add("&c --user, -user <username/uuid> &f- Removes entries from a single user only");
            helpItems.add("&c --read-only, -r &f- Removes only read entries, default behaviour");
            helpItems.add("&c --all, -a &f- Removes all entries");
        }
        if (sender.hasPermission("csn.command.upload")) {
            helpItems.add("&7- /csn &cupload &f- Force update databases");
        }
        if (sender.hasPermission("csn.command.convert")) {
            helpItems.add("&7- /csn &cconvert &f- Convert database to UUIDs");
        }

        if (sender.hasPermission("csn.command.reload")) {
            helpItems.add("&7- /csn &creload &f- Reload configuration");
        }

        for(String item : helpItems) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', item));
        }

    }

}
