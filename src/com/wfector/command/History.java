package com.wfector.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import code.husky.mysql.MySQL;

import com.wfector.util.ItemConverter;
import com.wfector.util.Time;

public class History {

	private String userName = "";
	private Integer maxRows = 25;

	private ArrayList<String> historyUsers = new ArrayList<String>();
	private ArrayList<String> historyItems = new ArrayList<String>();
	private ArrayList<Integer> historyAmounts = new ArrayList<Integer>();
	private ArrayList<Integer> historyTimes = new ArrayList<Integer>();
	private ArrayList<Integer> historyModes = new ArrayList<Integer>();
	private ArrayList<Integer> historyQuantities = new ArrayList<Integer>();
	
	private int index = 0;
	
	public void setUserName(String un) {
		this.userName = un;
	}
	public void setMaxRows(Integer mr) {
		this.maxRows = mr;
	}
	
	public void gatherResults(MySQL m) throws SQLException {
		Connection c = m.openConnection();
		Statement statement = c.createStatement();
		
		ResultSet res = statement.executeQuery("SELECT * FROM `csn` WHERE `ShopOwner`='" + this.userName + "' AND `Unread`='0' ORDER BY `Id` DESC LIMIT 1000");
		
		while(res.next()) {
			index++;
			
			historyUsers.add(res.getString("Customer"));
			historyItems.add(res.getString("ItemId"));
			historyAmounts.add(res.getInt("Amount"));
			historyTimes.add(res.getInt("Time"));
			historyModes.add(res.getInt("Mode"));
			historyQuantities.add(res.getInt("Quantity"));
		}
	}
	
	public Integer HasData(ArrayList<String[]> data, String[] search) {
		int v = 0;
		for(String[] arr : data) {
			int i = 0;
			boolean match = false;
			
			for(String s : search) {
				if(i != 3) {
					if(arr[i].equalsIgnoreCase(s)) {
						match = true;
					}
					else {
						//System.out.println("[Matching] '" + arr[i] + "' does not match '" + s + "'");
						match = false;
					}
				}
				i++;
			}
			
			if(match == true) {
				return v;
			}
			
			v++;
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
		
		for(String userName : historyUsers) {
			Integer amount = historyAmounts.get(index);
			String itemId = historyItems.get(index);
			Integer time = historyTimes.get(index);
			Integer mode = historyModes.get(index);
			Integer quantity = historyQuantities.get(index);

			itemId = ItemConverter.GetItemName(itemId);
			
			String[] arr = {
				userName,
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
				
				Integer Money = Integer.parseInt(arr[1]) * Multiplier;
				
				String msgString = "+ ";
				msgString += ChatColor.BLUE + arr[0] + " ";
				msgString += ChatColor.GRAY + "bought ";
				msgString += ChatColor.GREEN + (totalBought.toString()) + "x";
				msgString += ChatColor.BLUE + arr[2].replace(" ", "") + " ";
				msgString += ChatColor.WHITE + Time.GetAgo(Integer.parseInt(arr[3])) + " ago ";
				msgString += ChatColor.GRAY + "(+ $" + Money.toString() + ")";
				
				sender.sendMessage(msgString);
			}
			if(Integer.parseInt(arr[4]) == 2) {
				Integer totalBought = Integer.parseInt(arr[5]);
				totalBought = totalBought * (Multiplier);
				
				Integer Money = Integer.parseInt(arr[1]) * Multiplier;
				
				String msgString = "- ";
				msgString += ChatColor.BLUE + arr[0] + " ";
				msgString += ChatColor.GRAY + "sold you ";
				msgString += ChatColor.GREEN + (totalBought.toString()) + "x";
				msgString += ChatColor.BLUE + arr[2].replace(" ", "") + " ";
				msgString += ChatColor.WHITE + Time.GetAgo(Integer.parseInt(arr[3])) + " ago ";
				msgString += ChatColor.GRAY + "(- $" + Money.toString() + ")";
				
				sender.sendMessage(msgString);
			}
			
			i++;
		}
		
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c- To mark these as read, type /csn clear"));
		
		
	}
	
}
