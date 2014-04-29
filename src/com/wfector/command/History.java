package com.wfector.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang.ArrayUtils;
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
	
	private ArrayList<String> messages = new ArrayList<String>();
	
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
	
	public void showResults(CommandSender sender) {
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GRAY + "Latest Commissions");
		sender.sendMessage("");
		
		index = 0;
		int lines = 0;
		String lastString = "";
		String lastRealString = "";
		Integer repeats = 0;
		
		for(String userName : historyUsers) {
			Integer amount = historyAmounts.get(index);
			String itemId = historyItems.get(index);
			Integer time = historyTimes.get(index);
			Integer mode = historyModes.get(index);
			Integer quantity = historyQuantities.get(index);
			
			if(lines < this.maxRows) {
				itemId = ItemConverter.GetItemName(itemId);
				
				String newMessage = "";
				
				if(mode == 1) {
					newMessage = ChatColor.YELLOW + userName + ChatColor.GRAY + " bought ";
					newMessage += ChatColor.AQUA + quantity.toString() + "x" + itemId;
					newMessage += ChatColor.GRAY + " for $" + amount.toString();
					newMessage += ChatColor.GRAY + " (" + (Time.GetAgo(time)) + " ago)";
				}
				if(mode == 2) {
					newMessage = ChatColor.YELLOW + userName + ChatColor.GRAY + " sold you ";
					newMessage += ChatColor.AQUA + quantity.toString() + "x" + itemId;
					newMessage += ChatColor.GRAY + " for $" + amount.toString();
					newMessage += ChatColor.GRAY + " (" + (Time.GetAgo(time)) + " ago)";
				}
				
				if(newMessage != "") {
					String strippedString1 = newMessage;
					Integer beginTimeLocation = strippedString1.indexOf("(");
					strippedString1 = strippedString1.substring(0, beginTimeLocation);
					
					if(lastString.equalsIgnoreCase(strippedString1)) {
						repeats++;
					}
					else {
						if(repeats > 0) {
							System.out.println("Match");
							
							repeats++;
							messages.add(lastRealString + ChatColor.GREEN + " [x" + repeats.toString() + "]");
							lines++;
							repeats = 0;
						}
						else {
							System.out.println("No Match");
							
							if(lastString != "") 
								messages.add(lastRealString);
							lines++;
						}
					}
					
					lastString = strippedString1;
					lastRealString = newMessage;
				}
				else {
					System.out.println("Error. An entry had an invalid mode (expected 1 or 2, got " + mode.toString() + ")");
				}
				
				index++;
			}
		}

		if(repeats > 0) {
			messages.add(lastRealString + ChatColor.GREEN + " [x" + repeats.toString() + "]");
			repeats = 0;
			lines++;
		}
		
		Collections.reverse(messages);
		
		for(String m : messages) {
			sender.sendMessage(m);
		}
		
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c- To mark these as read, type /csn clear"));
		
		
	}
	
}
