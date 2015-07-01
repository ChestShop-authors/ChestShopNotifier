package com.wfector.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import code.husky.mysql.MySQL;

import com.wfector.notifier.ChestShopNotifier;
import com.wfector.util.ItemConverter;
import com.wfector.util.Time;
import com.Acrobot.ChestShop.Economy.Economy;

public class History {

    private UUID userId;
    private Integer maxRows = 25;

    private ArrayList<UUID> historyUsers = new ArrayList<UUID>();
    private ArrayList<String> historyItems = new ArrayList<String>();
    private ArrayList<Double> historyAmounts = new ArrayList<Double>();
    private ArrayList<Integer> historyTimes = new ArrayList<Integer>();
    private ArrayList<Integer> historyModes = new ArrayList<Integer>();
    private ArrayList<Integer> historyQuantities = new ArrayList<Integer>();

    private int index = 0;
    private ChestShopNotifier plugin;

    public History(ChestShopNotifier plugin) {
        this.plugin = plugin;
    }

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
            historyAmounts.add(res.getDouble("Amount"));
            historyTimes.add(res.getInt("Time"));
            historyModes.add(res.getInt("Mode"));
            historyQuantities.add(res.getInt("Quantity"));
        }
    }

    public Integer HasData(ArrayList<String[]> data, String[] search) {

        int i = 0;

        for(String[] arr : data) {
            boolean match = arr[0].equals(search[0]) && arr[1].equals(search[1]) && arr[2].equals(search[2]) && arr[4].equals(search[4]);
            if(match)
                return i;
            i++;
        }

        return -1;
    }

    public void showResults(CommandSender sender) {
        if(plugin.getMessage("history-caption") != null) sender.sendMessage(plugin.getMessage("history-caption"));
        sender.sendMessage("");

        index = 0;
        int lines = 0;

        if(historyUsers.isEmpty()) {
            if(plugin.getMessage("history-empty") != null) sender.sendMessage(plugin.getMessage("history-empty"));

            return;
        }

        ArrayList<String[]> data = new ArrayList<String[]>();
        ArrayList<Integer> times = new ArrayList<Integer>();

        for(UUID userId : historyUsers) {
            Double amount = historyAmounts.get(index);
            String itemName = historyItems.get(index);
            Integer time = historyTimes.get(index);
            Integer mode = historyModes.get(index);
            Integer quantity = historyQuantities.get(index);

            itemName = ItemConverter.GetItemName(itemName);

            String[] arr = {
                    userId.toString(),
                    amount.toString(),
                    itemName,
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

            String msgString = (Integer.parseInt(arr[4]) == 1) ? plugin.getMessage("history-bought") : plugin.getMessage("history-sold");

            Integer totalItems = Integer.parseInt(arr[5]);
            totalItems = totalItems * (Multiplier);

            Double money = Double.parseDouble(arr[1]) * Multiplier;

            msgString = msgString.replace("{player}", Bukkit.getOfflinePlayer(UUID.fromString(arr[0])).getName());
            msgString = msgString.replace("{count}", totalItems.toString());
            msgString = msgString.replace("{item}", arr[2].replace(" ", ""));
            msgString = msgString.replace("{timeago}", Time.GetAgo(Integer.parseInt(arr[3])));
            msgString = msgString.replace("{money}", Economy.formatBalance(money));

            sender.sendMessage(msgString);

            i++;
        }

        sender.sendMessage(" ");
        sender.sendMessage(plugin.getMessage("history-read"));


    }

}
