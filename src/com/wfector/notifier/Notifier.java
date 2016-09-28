package com.wfector.notifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Notifier extends BukkitRunnable {
    private ChestShopNotifier plugin;

    public boolean newNotifications = false;

    ArrayList<UUID> notifyusers_ids = new ArrayList<UUID>();
    ArrayList<Integer> notifyusers_sales = new ArrayList<Integer>();
    ArrayList<Integer> notifyusers_times = new ArrayList<Integer>();

    public Notifier(ChestShopNotifier main) {
        this.plugin = main;
    }

    public void run() {
        if(!newNotifications) return;
        if(!plugin.isPluginEnabled()) return;

        for(int i = 0; i < notifyusers_ids.size(); i++) {
            int sales = notifyusers_sales.get(i);
            UUID userid = notifyusers_ids.get(i);

            Player p = plugin.getServer().getPlayer(userid);
            if(p != null) {
                plugin.debug("Ran for user '" + p.getName() + "'");
                if(plugin.getMessage("sales") != null) {
                    p.sendMessage(plugin.getMessage("sales").replace("{sales}", String.valueOf(sales)));
                }

                if(plugin.getMessage("history-cmd") != null) {
                    p.sendMessage(plugin.getMessage("history-cmd"));
                }
            } else {
                plugin.debug("Warning: The player with the uuid '" + userid + "' could not be found, yet was in queue.");
            }
        }

        plugin.debug("Finished.");
        notifyusers_ids.clear();
        notifyusers_sales.clear();
        notifyusers_times.clear();

        newNotifications = false;
    }

    public void add(UUID pId, int amount, int sendTime) {
        notifyusers_ids.add(pId);
        notifyusers_sales.add(amount);
        notifyusers_times.add(sendTime);
        newNotifications = true;
    }
}