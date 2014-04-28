package com.wfector.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Help {

	public static void SendDialog(CommandSender sender) {
		
		String[] helpItems = {
			"&c--------------- ChestShop Notifier ---------------",
			"&c ",
			"&c /csn history   &7See all of your recent transactions",
			"&c /csn clear     &7Clear all transaction history"
		};
		
		for(String item : helpItems) {
			if(item == "&c /csn history   &7See all of your recent transactions") {
				if(sender.isOp()) {
					item = "&c /csn history (username)  &7See all of your recent transactions";
				}
			}
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', item));
		}
		
	}
	
}
