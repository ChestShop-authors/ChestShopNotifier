package com.wfector.command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import com.Acrobot.ChestShop.UUIDs.NameManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class Converter extends BukkitRunnable {

    private final ChestShopNotifier plugin;
    private final CommandSender sender;

    private HashMap<String,UUID> uuidmap = new HashMap<String,UUID>();

    public Converter(ChestShopNotifier plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public void run() {
        if (convertDatabase()) {
            sender.sendMessage(plugin.getMessage("database-converted"));
            plugin.getLogger().log(Level.INFO, "Database converted!");
        } else {
            sender.sendMessage(plugin.getMessage("database-convert-fail"));
            plugin.getLogger().log(Level.SEVERE, "Error while trying to convert! Maybe you don't have a 'csn' table?");
        }
    }

    private boolean convertDatabase() {
        try (Connection conn = plugin.getConnection()){

            Statement sta = conn.createStatement();
            ResultSet res = sta.executeQuery("SELECT * FROM `csn` WHERE `Unread`='0' ORDER BY `Id` ASC");

            while(res.next()) {

                String shopOwner = res.getString("ShopOwner");
                String costumer = res.getString("Customer");

                UUID shopOwnerId = this.getPlayerID(shopOwner);
                UUID customerId = this.getPlayerID(costumer);

                if(shopOwnerId != null && customerId != null) {
                    PreparedStatement prepsta = conn.prepareStatement("INSERT INTO csnUUID (`ShopOwnerId`, `CustomerId`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

                    prepsta.setString(1, shopOwnerId.toString());
                    prepsta.setString(2, customerId.toString());
                    prepsta.setString(3, res.getString("ItemId"));
                    prepsta.setString(4, String.valueOf(res.getInt("Mode")));
                    prepsta.setString(5, String.valueOf(res.getInt("Amount")));
                    prepsta.setString(6, String.valueOf(res.getInt("Time")));
                    prepsta.setString(7, String.valueOf(res.getInt("Quantity")));
                    prepsta.setString(8, String.valueOf(res.getInt("Unread")));

                    prepsta.execute();
                }
            }
            conn.createStatement().executeUpdate("ALTER TABLE `csn` RENAME TO `csnOLD`");

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        uuidmap.clear();
        return true;

    }

    private UUID getPlayerID(String playername) {
        UUID playerid;
        if(uuidmap.containsKey(playername.toLowerCase())) {
            playerid = uuidmap.get(playername.toLowerCase());
        } else {
            playerid = NameManager.getUUID(playername);
            if (playerid == null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(playername);
                if (op == null) {
                    plugin.getLogger().log(Level.SEVERE, "Could not get the player " + playername);
                    return null;
                }
                playerid = op.getUniqueId();
            }
            if(playerid == null || playerid.version() != 4) {
                plugin.getLogger().log(Level.WARNING, "Could not find a player with the username " + playername);
                return null;
            }
            uuidmap.put(playername.toLowerCase(),playerid);
        }

        return playerid;
    }

}
