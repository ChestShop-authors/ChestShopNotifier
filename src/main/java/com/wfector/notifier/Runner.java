package com.wfector.notifier;

import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

public class Runner extends BukkitRunnable {
    private ChestShopNotifier plugin;

    public Runner(ChestShopNotifier main) {
        this.plugin = main;
    }

    public void run() {
        try {
            this.plugin.runBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
