package com.wfector.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import code.husky.mysql.MySQL;

import com.Acrobot.ChestShop.ChestShop;
import com.wfector.util.ItemConverter;
import com.wfector.util.Time;

import com.Acrobot.ChestShop.Economy.Economy;

public class History {

	private UUID userId;
	private Integer maxRows = 25;

	private ArrayList<UUID> historyUsers = new ArrayList<UUID>();
	private ArrayList<String> historyItems = new ArrayList<String>();
	private ArrayList<Integer> historyAmounts = new ArrayList<Integer>();
	private ArrayList<Integer> historyTimes = new ArrayList<Integer>();
	private ArrayList<Integer> historyModes = new ArrayList<Integer>();
	private ArrayList<Integer> historyQuantities = new ArrayList<Integer>();
	
	private int index = 0;
	
	public void setUserId(UUID uid) {
		this.userId = uid;
	}
	public void setMaxRows(Integer mr) {
		this.maxRows = mr;
	}
	
	public void gatherResults(MySQL m) throws SQLException {
		Connection c = m.openConnection();
		Statement statement = c.createStatement();
		
		ResultSet res = statement.executeQuery("SELECT * FROM `csnUUID` WHERE `ShopOwnerId`='" + this.userId.toString() + "' AND `Unread`='0' ORDER BY `Id` DESC LIMIT 1000");
		
		while(res.next()) {
			index++;
			
			historyUsers.add(UUID.fromString(res.getString("CustomerId")));
			historyItems.add(res.getString("ItemId"));
			historyAmounts.add(res.getInt("Amount"));
			historyTimes.add(res.getInt("Time"));
			historyModes.add(res.getInt("Mode"));
			historyQuantities.add(res.getInt("Quantity"));
		}
	}
	
	public Integer HasData(ArrayList<String[]> data, String[] search) {
		
		int i = 0;
		
		for(String[] arr : data) {
			boolean match = false;
	
			if(arr[0].equalsIgnoreCase(search[0])) match = true;
			else match = false;
			
			if(arr[1].equalsIgnoreCase(search[1]) && match) match = true;
			else match = false;
			
			if(arr[4].equalsIgnoreCase(search[4]) && match) match = true;
			else match = false;
			
			if(match) return i;
				
			i++;
		}
		
		return -1;
	}
	
	public void showResults(CommandSender sender) {
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GRAY + "Latest Commissions");
		sender.sendMessage("");
		
		index = 0;
		int lines = 0;
		
		if(historyUsers.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "Nothing to show.");
			
			return;
		}
		
		ArrayList<String[]> data = new ArrayList<String[]>();
		ArrayList<Integer> times = new ArrayList<Integer>();
		
		for(UUID userId : historyUsers) {
			Integer amount = historyAmounts.get(index);
			String itemId = historyItems.get(index);
			Integer time = historyTimes.get(index);
			Integer mode = historyModes.get(index);
			Integer quantity = historyQuantities.get(index);

			itemId = ItemConverter.GetItemName(itemId);
			
			String[] arr = {
				userId.toString(),
				amount.toString(),
				itemId,
				time.toString(),
				mode.toString(),
				quantity.toString()
			};
			
			if(lines < this.maxRows) {
				Integer hasData = HasData(data, arr);
				if(hasData > -1) {
					times.set(hasData, times.get(hasData) + 1);
				}
				else {
					data.add(arr);
					times.add(1);
					lines++;
				}
				
				index++;
			}
		}

		Collections.reverse(data);
		Collections.reverse(times);
		
		int i = 0;
		
		for(String[] arr : data) {
			Integer Multiplier = times.get(i);

			if(Integer.parseInt(arr[4]) == 1) {
				Integer totalBought = Integer.parseInt(arr[5]);
				totalBought = totalBought * (Multiplier);
				
				Integer money = Integer.parseInt(arr[1]) * Multiplier;
				
				String msgString = "+ ";
				msgString += ChatColor.BLUE + Bukkit.getOfflinePlayer(UUID.fromString(arr[0])).getName() + " ";
				msgString += ChatColor.GRAY + "bought ";
				msgString += ChatColor.GREEN + (totalBought.toString()) + "x";
				msgString += ChatColor.BLUE + arr[2].replace(" ", "") + " ";
				msgString += ChatColor.WHITE + Time.GetAgo(Integer.parseInt(arr[3])) + " ago ";
				msgString += ChatColor.GRAY + "(+" + Economy.formatBalance(money) + ")";
				
				sender.sendMessage(msgString);
			}
			if(Integer.parseInt(arr[4]) == 2) {
				Integer totalBought = Integer.parseInt(arr[5]);
				totalBought = totalBought * (Multiplier);
				
				Integer money = Integer.parseInt(arr[1]) * Multiplier;
				
				String msgString = "- ";
				msgString += ChatColor.BLUE + Bukkit.getOfflinePlayer(UUID.fromString(arr[0])).getName() + " ";
				msgString += ChatColor.GRAY + "sold you ";
				msgString += ChatColor.GREEN + (totalBought.toString()) + "x";
				msgString += ChatColor.BLUE + arr[2].replace(" ", "") + " ";
				msgString += ChatColor.WHITE + Time.GetAgo(Integer.parseInt(arr[3])) + " ago ";
				msgString += ChatColor.GRAY + "(-" + Economy.formatBalance(money) + ")";
				
				sender.sendMessage(msgString);
			}
			
			i++;
		}
		
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c- To mark these as read, type /csn clear"));
		
		
	}
	
}
