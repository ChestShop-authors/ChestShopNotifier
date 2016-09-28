package com.wfector.notifier;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.bukkit.scheduler.BukkitRunnable;

public class BatchRunner extends BukkitRunnable {
    private ChestShopNotifier plugin;

    public BatchRunner(ChestShopNotifier main) {
        this.plugin = main;
    }

    @Override
    public void run() {
        plugin.debug("Uploading a batch...");

        if(plugin.getBatch().isEmpty()) return;
        if(!plugin.isPluginEnabled()) return;

        if(plugin.getBatch().size() > 0) {

            Connection conn = plugin.getConnection();
            if (conn == null) {
                plugin.getLogger().log(Level.WARNING, "Invalid database connection!");
                return;
            }

            try {
                String qstr = "INSERT INTO csnUUID (`ShopOwnerId`, `CustomerId`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES ";

                int i = 0;

                for(String query : plugin.getBatch()) {
                    qstr += query;
                    if(plugin.getBatch().size() > (i+1)) {
                        qstr += ", ";
                    }
                    i++;
                }

                Statement statement = conn.createStatement();
                statement.executeUpdate(qstr);
                plugin.debug("Update: " + qstr);

                plugin.getBatch().clear();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        plugin.debug("Batch completed.");
    }
}
