package com.wfector.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.scheduler.BukkitRunnable;

public class Clear extends BukkitRunnable {

    private final ChestShopNotifier plugin;
    private final UUID userId;

    public Clear(ChestShopNotifier plugin, UUID userId) {
        this.plugin = plugin;
        this.userId = userId;
    }

    public void run() {
        Connection c = null;
        try {
            c = plugin.getConnection();
            Statement statement = c.createStatement();
            statement.executeUpdate("UPDATE csnUUID SET `Unread`='1' WHERE `ShopOwnerId`='" + userId.toString() + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ChestShopNotifier.close(c);
        }

    }
}
