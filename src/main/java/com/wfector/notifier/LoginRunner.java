package com.wfector.notifier;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;

public class LoginRunner extends BukkitRunnable {
    private final ChestShopNotifier plugin;
    private final Player player;

    public LoginRunner(ChestShopNotifier plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {

        Connection conn = plugin.getConnection();
        if (conn == null) {
            plugin.getLogger().log(Level.WARNING, "Invalid database connection!");
            return;
        }

        Statement statement;
        ResultSet res;
        try {
            statement = conn.createStatement();

            res = statement.executeQuery("SELECT `ShopOwnerId` FROM csnUUID WHERE `ShopOwnerId`='" + player.getUniqueId().toString() + "' AND `Unread`='0'");

            res.next();

            int amount = 0;
            if (res.getMetaData().getColumnCount() > 0)
                while (res.next())
                    amount++;

            plugin.debug("Found rows: " + String.valueOf(amount));

            if (amount > 0 && player.isOnline()) {
                Date dt = new Date();
                plugin.debug("Added message to queue (delay s: " + plugin.getJoinNotificationDelay() + ")");
                int sendTime = (int) (dt.getTime() / 1000) + plugin.getJoinNotificationDelay();

                plugin.getNotifier().add(player.getUniqueId(), amount, sendTime);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        plugin.debug("Done.");
    }
}
