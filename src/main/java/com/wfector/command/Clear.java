package com.wfector.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Clear extends BukkitRunnable {

    private final ChestShopNotifier plugin;
    private final CommandSender sender;

    public Clear(ChestShopNotifier plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public void run() {
        UUID senderId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : NameManager.getUUID(Properties.ADMIN_SHOP_NAME);
        try (Connection c = plugin.getConnection()){
            Statement statement = c.createStatement();
            statement.executeUpdate("DELETE FROM csnUUID WHERE `Unread`='1' AND `ShopOwnerId`='" + senderId.toString() + "'");

            sender.sendMessage(plugin.getMessage("history-clear"));
        } catch (SQLException e) {
            sender.sendMessage(plugin.getMessage("database-error-oncommand"));
            e.printStackTrace();
        }

    }
}
