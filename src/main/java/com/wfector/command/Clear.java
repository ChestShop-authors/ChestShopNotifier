package com.wfector.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.wfector.notifier.ChestShopNotifier;
import org.bukkit.ChatColor;
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
        Connection c = null;
        try {
            c = plugin.getConnection();
            Statement statement = c.createStatement();
            statement.executeUpdate("DELETE FROM csnUUID WHERE `Unread`='0' AND `ShopOwnerId`='" + senderId.toString() + "'");

            if(plugin.getMessage("history-clear") != null) sender.sendMessage(plugin.getMessage("history-clear"));
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Database error while executing this command!");
            e.printStackTrace();
        } finally {
            ChestShopNotifier.close(c);
        }

    }
}
