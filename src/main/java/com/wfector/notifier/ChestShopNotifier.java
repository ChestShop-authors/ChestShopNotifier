package com.wfector.notifier;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.wfector.command.CommandRunner;
import com.wfector.util.Time;

import code.husky.mysql.MySQL;
import org.bukkit.scheduler.BukkitRunnable;

import static com.Acrobot.Breeze.Utils.MaterialUtil.getSignName;

public class ChestShopNotifier extends JavaPlugin implements Listener {

    public MySQL MySQL;

    private ArrayList<String> batch = new ArrayList<String>();

    private boolean verboseEnabled;
    private boolean joinNotificationEnabled;
    private int joinNotificationDelay;

    private Connection conn;

    public boolean pluginEnabled = false;
    public boolean logAdminShop = true;

    private Notifier notifier;

    public void onEnable() {
        getCommand("csn").setExecutor(new CommandRunner(this));

        saveDefaultConfig();
        updateConfiguration(null, false);

        getLogger().log(Level.INFO, "Connecting to the database...");

        new BukkitRunnable() {
            public void run() {
                Connection c = getConnection();
                if (c == null) {
                    getLogger().log(Level.WARNING, "Failed to connect to the database! Disabling connections!");
                    return;
                }

                try {
                    Statement statement = c.createStatement();

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS csnUUID (Id int(11) AUTO_INCREMENT, ShopOwnerId VARCHAR(36), CustomerId VARCHAR(36), ItemId VARCHAR(1000), Mode INT(11), Amount FLOAT(53), Quantity INT(11), Time INT(11), Unread INT(11), PRIMARY KEY (Id))");

                    getLogger().log(Level.INFO, "Connected!");
                    pluginEnabled = true;
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        c.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskAsynchronously(this);

        notifier = new Notifier(this);
        notifier.runTaskTimer(this, 60, 60);

        getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable() {
        if(batch.size() > 0 && getConnection() != null) {
            getLogger().log(Level.INFO, "Database queue is not empty. Uploading now...");
            new BatchRunner(this).run();
            getLogger().log(Level.INFO, "Done uploading database queue!");
        }

    }

    public boolean isPluginEnabled() {
        return isEnabled() && pluginEnabled;
    }

    public boolean updateConfiguration(final CommandSender sender, boolean isReload) {
        if(isReload) {
            reloadConfig();
        }

        verboseEnabled = getConfig().getBoolean("debugging.verbose");
        joinNotificationEnabled = getConfig().getBoolean("notifications.notify-on-user-join");
        joinNotificationDelay = getConfig().getInt("notifications.delay-seconds");

        String dbHost = getConfig().getString("database.host");
        int dbPort = getConfig().getInt("database.port");
        String dbName = getConfig().getString("database.dbname");
        String dbUsername = getConfig().getString("database.username");
        String dbPassword = getConfig().getString("database.password");

        logAdminShop = getConfig().getBoolean("logging.admin-shop");

        if(isReload) {
            MySQL = new MySQL(this, dbHost, String.valueOf(dbPort), dbName, dbUsername, dbPassword);

            getLogger().log(Level.INFO, "Connecting to the database...");

            new BukkitRunnable() {
                public void run() {
                    Connection c = getConnection();

                    pluginEnabled = c != null;
                    if(pluginEnabled) {
                        getLogger().log(Level.WARNING, "Database connected!");
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GREEN + "Reloaded!");
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GREEN + "Database connected!");

                    } else {
                        getLogger().log(Level.WARNING, "Failed to connect to the database! Disabling connections!");
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.GREEN + "Reloaded!");
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "ChestShop Notifier // " + ChatColor.RED + "Database failed to connect!");
                    }
                }
            }.runTaskAsynchronously(this);
        }
        return true;
    }

    /**
     * Gets a message from the config file.
     * @param string The name of the message to get
     * @return The message or null if it doesn't exist
     */
    public String getMessage(String string) {
        return (getConfig().contains("messages." + string)) ? ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + string)) : null;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        if(!joinNotificationEnabled) {
            debug("Join notifications are " + joinNotificationEnabled + ", skipping...");
            return;
        }

        debug("User joined. Checking for updates...");

        if(!isPluginEnabled()) {
            debug("Cannot notify user. Plugin is disabled.");
            return;
        }

        final Player p = e.getPlayer();

        new LoginRunner(this, p).runTaskAsynchronously(this);
    }

    @EventHandler
    public boolean onChestShopTransaction(TransactionEvent e) {
        UUID ownerId = e.getOwner().getUniqueId();

        if(!this.logAdminShop && NameManager.isAdminShop(ownerId)) return true;

        TransactionType f = e.getTransactionType();

        Integer mode = (f == TransactionType.BUY) ? 1 : 2;

        double price = e.getPrice();
        UUID clientId = e.getClient().getUniqueId();

        StringBuilder items = new StringBuilder(50);
        int itemQuantities = 0;

        for (ItemStack item : e.getStock()) {
            items.append(getSignName(item));
            itemQuantities = item.getAmount();
        }

        String itemId = items.toString();

        batch.add("('" + ownerId.toString() + "', '" + clientId.toString() + "', '" + itemId + "', '" + mode.toString() + "', '" + String.valueOf(price) + "', '" + Time.GetEpochTime() + "', '" + String.valueOf(itemQuantities) + "', '0')");

        debug("Item added to batch.");
        new BatchRunner(this).runTaskAsynchronously(this);

        return true;
    }

    public boolean connect() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = MySQL.openConnection();
            }
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "Could not establish database connection!", e);
            return false;
        }
        return true;
    }

    public void debug(String d) {
        if(verboseEnabled)
            getLogger().log(Level.INFO, d);
    }

    public Connection getConnection() {
        if(!connect()) return null;
        return conn;

    }

    public Notifier getNotifier() {
        return notifier;
    }

    public ArrayList<String> getBatch() {
        return batch;
    }

    public int getJoinNotificationDelay() {
        return joinNotificationDelay;
    }
}
