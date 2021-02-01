package com.wfector.notifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

            try (Connection conn = plugin.getConnection()){
                conn.setAutoCommit(false);
                String qstr = "INSERT INTO csnUUID (`ShopOwnerId`, `CustomerId`, `CustomerName`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES (?,?,?,?,?,?,?,?,?)";

                PreparedStatement statement = conn.prepareStatement(qstr);

                for(int i = 0; i < plugin.getBatch().size(); i++) {
                    Object[] values = plugin.getBatch().get(i);

                    for (int j = 0; j < statement.getParameterMetaData().getParameterCount(); j++) {
                        statement.setObject(j + 1, values[j]);
                    }
                    statement.addBatch();
                    if (i % 1000 == 0 || i + 1 == plugin.getBatch().size()) {
                        statement.executeBatch(); // Execute every 1000 items.
                        conn.commit();
                    }
                }

                plugin.debug("Update: " + qstr);

                plugin.getBatch().clear();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        plugin.debug("Batch completed.");
    }
}
