package com.wfector.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.wfector.notifier.ChestShopNotifier;

public class Converter {

    private ChestShopNotifier plugin = null;
    private Connection conn = null;

    private HashMap<String,UUID> uuidmap = new HashMap<String,UUID>();

    public Converter(ChestShopNotifier plugin, Connection conn) {
        this.plugin = plugin;
        this.conn = conn;
    }

    public boolean convertDatabase() {
        try {
            Statement sta = this.conn.createStatement();
            ResultSet res = sta.executeQuery("SELECT * FROM `csn` WHERE `Unread`='0' ORDER BY `Id` ASC");

            while(res.next()) {

                String shopOwner = res.getString("ShopOwner");
                String costumer = res.getString("Customer");

                UUID shopOwnerId = this.getPlayerID(shopOwner);
                UUID customerId = this.getPlayerID(costumer);

                if(shopOwnerId != null && customerId != null) {
                    PreparedStatement prepsta = this.conn.prepareStatement("INSERT INTO csnUUID (`ShopOwnerId`, `CustomerId`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

                    prepsta.setString(1, shopOwnerId.toString());
                    prepsta.setString(2, customerId.toString());
                    prepsta.setString(3, res.getString("ItemId"));
                    prepsta.setString(4, String.valueOf(res.getInt("Mode")));
                    prepsta.setString(5, String.valueOf(res.getInt("Amount")));
                    prepsta.setString(6, String.valueOf(res.getInt("Time")));
                    prepsta.setString(7, String.valueOf(res.getInt("Quantity")));
                    prepsta.setString(8, String.valueOf(res.getInt("Unread")));

                    prepsta.execute();

                    prepsta.close();

                }
            }

            this.conn.createStatement().executeUpdate("ALTER TABLE `csn` RENAME TO `csnOLD`");

        } catch (SQLException e) {
            // TODO Auto-generated catch block
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
            OfflinePlayer op = Bukkit.getOfflinePlayer(playername);
            if(op == null) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not get the player " + playername);
                return null;
            }
            playerid = op.getUniqueId();
            if(playerid == null || playerid.version() != 4) {
                this.plugin.getLogger().log(Level.WARNING, "Could not find a player with the username " + playername);
                return null;
            }
            uuidmap.put(playername.toLowerCase(),playerid);
        }

        return playerid;
    }

}
