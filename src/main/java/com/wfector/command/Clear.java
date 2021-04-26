package com.wfector.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class Clear extends BukkitRunnable {

    private final ChestShopNotifier plugin;
    private final CommandSender sender;
    private final UUID userId;

    public Clear(ChestShopNotifier plugin, CommandSender sender, UUID userId) {
        this.plugin = plugin;
        this.sender = sender;
        this.userId = userId;
    }

    public void run() {
        try (Connection c = plugin.getConnection()){
            Statement statement = c.createStatement();
            statement.executeUpdate("DELETE FROM csnUUID WHERE `Unread`='1' AND `ShopOwnerId`='" + userId.toString() + "'");

            sender.sendMessage(plugin.getMessage("history-clear"));
        } catch (SQLException e) {
            sender.sendMessage(plugin.getMessage("database-error-oncommand"));
            e.printStackTrace();
        }

    }
}
