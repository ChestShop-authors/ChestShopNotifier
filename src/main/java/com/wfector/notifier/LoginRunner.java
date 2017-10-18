package com.wfector.notifier;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class LoginRunner extends BukkitRunnable {
    private final ChestShopNotifier plugin;
    private final UUID playerId;

    public LoginRunner(ChestShopNotifier plugin, UUID playerId) {
        this.plugin = plugin;
        this.playerId = playerId;
    }

    @Override
    public void run() {
        Connection conn = null;
        Player p = plugin.getServer().getPlayer(playerId);
        if(p == null || !p.isOnline()) {
            // player is no longer online
            return;
        }
        try {
            conn = plugin.getConnection();
            Statement statement = conn.createStatement();

            ResultSet res = statement.executeQuery("SELECT `ShopOwnerId` FROM csnUUID WHERE `ShopOwnerId`='" + playerId.toString() + "' AND `Unread`='0'");

            res.next();

            int amount = 0;
            if (res.getMetaData().getColumnCount() > 0)
                while (res.next())
                    amount++;

            plugin.debug("Found rows: " + String.valueOf(amount));

            if (amount > 0) {
                plugin.debug("Ran for user '" + p.getName() + "'");
                if(plugin.getMessage("sales") != null) {
                    p.sendMessage(plugin.getMessage("sales").replace("{sales}", String.valueOf(amount)));
                }

                if(plugin.getMessage("history-cmd") != null) {
                    p.sendMessage(plugin.getMessage("history-cmd"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ChestShopNotifier.close(conn);
        }
        plugin.debug("Done.");
    }
}
