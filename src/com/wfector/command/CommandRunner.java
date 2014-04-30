package com.wfector.command;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wfector.notifier.Main;

public class CommandRunner {
	private Main plugin;
	
	public void SetPlugin(Main m) {
		this.plugin = m;
	}

	@SuppressWarnings("deprecation")
	public void Process(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCommand usage: /csn help"));
			return;
		}
		else {
			if(args[0].equalsIgnoreCase("reload") && (sender.hasPermission("csn.admin") || sender.isOp())) {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GRAY + "Reloading, please wait...");
				
				boolean didUpdate = this.plugin.updateConfiguration(true);
				if(didUpdate) {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GREEN + "Reloaded!");
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GREEN + "Database connected!");
					
				}
				else {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GREEN + "Reloaded!");
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.RED + "Database failed to connect!");
				}
				
				return;
			}
			if(args[0].equalsIgnoreCase("help") && sender.hasPermission("csn.user")) {
				Help.SendDialog(sender);
				return;
			}
			if(args[0].equalsIgnoreCase("history") && sender.hasPermission("csn.user")) {
				Player target;
				
				if(this.plugin.pluginEnabled == false) {
					sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
					return;
				}
				
				if(args.length > 1) {
					if(args.length > 2) {
						sender.sendMessage(ChatColor.RED + "Too many arguments! /csn history [username]");
						return;
					}
					
					if(sender.hasPermission("csn.history.others") || sender.isOp() || sender.hasPermission("csn.admin")) {
						target = Bukkit.getPlayer(args[1]);
					}
					else {
						sender.sendMessage(ChatColor.RED + "Too many arguments! /csn history");
						return;
					}
				}
				else {
					target = (Player) sender;
				}
				
				if(target == null) {
					sender.sendMessage(ChatColor.RED + "The user '" + args[1] + "' was not found.");
					return;
				}
				
				String userName = target.getName();
				
				History csh = new History();
				csh.setUserName(userName);
				
				try {
					csh.gatherResults(this.plugin.MySQL);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				csh.showResults(sender);
				
				return;
			}
			if(args[0].equalsIgnoreCase("upload") && sender.hasPermission("csn.admin")) {
				if(this.plugin.pluginEnabled == false) {
					sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
					return;
				}
				
				try {
					this.plugin.runBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				sender.sendMessage(ChatColor.RED + "Batch executed!");
				
				return;
			}
			if(args[0].equalsIgnoreCase("clear") && sender.hasPermission("csn.user")) {
				if(this.plugin.pluginEnabled == false) {
					sender.sendMessage(ChatColor.RED + "Invalid database connection. Please edit config and /csn reload.");
					return;
				}
				
				Clear.ClearHistory(this.plugin.MySQL, sender.getName());
				sender.sendMessage(ChatColor.RED + "History cleared! New sales will continue to be recorded.");
				
				return;
			}
		}
		
		sender.sendMessage(ChatColor.RED + "Command not recognized. Type /csn help for help.");
		return;
	}
}
