package com.wfector.notifier;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.io.File;

import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Utils.ItemUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.wfector.command.CommandRunner;
import com.wfector.util.Time;

import org.bukkit.scheduler.BukkitRunnable;

public class ChestShopNotifier extends JavaPlugin implements Listener {

    private HikariDataSource ds;
    private DbType dbType = DbType.SQLITE;

    private Queue<HistoryEntry> batch = new ArrayDeque<>();

    private boolean verboseEnabled;
    private boolean joinNotificationEnabled;
    private int joinNotificationDelay;

    public boolean pluginEnabled = false;
    public boolean logAdminShop = true;

    public Cache<UUID, String> playerNames = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

    public void onEnable() {
        getCommand("csn").setExecutor(new CommandRunner(this));

        saveDefaultConfig();
        updateConfiguration(null);

        if (getConfig().getBoolean("clean-on-startup.enabled") && getConfig().getString("clean-on-startup.command", null) != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    getLogger().log(Level.INFO, "Automatic database cleaning on startup is enabled!");
                    String parameters = getConfig().getString("clean-on-startup.parameters").trim();
                    getLogger().log(Level.INFO, "Parameters: " + parameters);
                    getServer().dispatchCommand(getServer().getConsoleSender(), "csn cleandatabase " + parameters);
                }
            }.runTaskLater(this, 200);
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable() {
        if(batch.size() > 0) {
            getLogger().log(Level.INFO, "Database queue is not empty. Uploading now...");
            new BatchRunner(this).run();
            getLogger().log(Level.INFO, "Done uploading database queue!");
        }
        ds.close();

    }

    public boolean isPluginEnabled() {
        return isEnabled() && pluginEnabled;
    }

    public void updateConfiguration(final CommandSender sender) {
        verboseEnabled = getConfig().getBoolean("debugging.verbose");
        joinNotificationEnabled = getConfig().getBoolean("notifications.notify-on-user-join");
        joinNotificationDelay = getConfig().getInt("notifications.delay-seconds");
        logAdminShop = getConfig().getBoolean("logging.admin-shop");

        String dbType = getConfig().getString("database.type", "mysql");
        try {
            this.dbType = DbType.valueOf(dbType.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.dbType = DbType.SQLITE;
            getLogger().log(Level.WARNING, "Unknown dbType setting '" + dbType + "'! Possible settings are MySQL and SQLite. Falling back to SQLite!");
        }
        String dbHost = getConfig().getString("database.host");
        int dbPort = getConfig().getInt("database.port");
        String dbName = getConfig().getString("database.dbname", "database");
        String dbUsername = getConfig().getString("database.username");
        String dbPassword = getConfig().getString("database.password");
        boolean useSsl = getConfig().getBoolean("database.ssl");

        ds = new HikariDataSource();

        switch (this.dbType) {
            default:
                getLogger().log(Level.WARNING, "Unsupported database type setting '" + this.dbType + "'! Falling back to SQLite!");
            case SQLITE:
                ds.setJdbcUrl("jdbc:sqlite:" + new File(getDataFolder(), dbName + ".sqlite"));
                break;
            case MYSQL:
                ds.setJdbcUrl("jdbc:" + dbType + "://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=" + useSsl);
                break;
        }
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setConnectionTimeout(5000);

        getLogger().log(Level.INFO, "Connecting to the database...");

        new BukkitRunnable() {
            public void run() {
                try (Connection c = getConnection()){
                    Statement statement = c.createStatement();

                    switch (ChestShopNotifier.this.dbType) {
                        default:
                        case SQLITE:
                            statement.executeUpdate("CREATE TABLE IF NOT EXISTS csnUUID (Id INTEGER PRIMARY KEY AUTOINCREMENT, ShopOwnerId VARCHAR(36), CustomerId CHAR(36), CustomerName VARCHAR(16), ItemId VARCHAR(1000), Mode INT(11), Amount FLOAT(53), Quantity INT(11), Time INT(11), Unread INT(11))");
                            break;
                        case MYSQL:
                            statement.executeUpdate("CREATE TABLE IF NOT EXISTS csnUUID (Id int(11) AUTO_INCREMENT, ShopOwnerId VARCHAR(36), CustomerId CHAR(36), CustomerName VARCHAR(16), ItemId VARCHAR(1000), Mode INT(11), Amount FLOAT(53), Quantity INT(11), Time INT(11), Unread INT(11), PRIMARY KEY (Id))");
                            break;
                    }

                    pluginEnabled = true;
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try (Connection c = getConnection()){
                    Statement statement = c.createStatement();

                    switch (ChestShopNotifier.this.dbType) {
                        default:
                        case SQLITE:
                            statement.executeUpdate("ALTER TABLE csnUUID ADD COLUMN CustomerName VARCHAR(16)");
                            break;
                        case MYSQL:
                            statement.executeUpdate("ALTER TABLE csnUUID ADD COLUMN CustomerName VARCHAR(16) AFTER CustomerId");
                            break;
                    }
                } catch (SQLException e) {
                    if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                        e.printStackTrace();
                    }
                }

                if(pluginEnabled) {
                    getLogger().log(Level.INFO, "Database connected!");
                    if (sender != null) {
                        sender.sendMessage(getMessage("reload-success"));
                        sender.sendMessage(getMessage("reload-database-success"));
                    }
                } else {
                    getLogger().log(Level.WARNING, "Failed to connect to the database! Disabling connections!");
                    if (sender != null) {
                        sender.sendMessage(getMessage("reload-success"));
                        sender.sendMessage(getMessage("reload-database-fail"));
                    }
                }
            }
        }.runTaskAsynchronously(this);
    }

    /**
     * returns a texty string
     *
     * @param key the config path
     * @param replacements Optional replacements. Use {index} in the message to address them.
     * @return the text
     */
    public String getMessage(String key, String... replacements) {
        String s = getConfig().getString("messages." + key);
        if (s != null && !s.isEmpty()) {
            for (int i = 0; i < replacements.length; i++) {
                if (i + 1 < replacements.length) {
                    s = s.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
            return ChatColor.translateAlternateColorCodes('&', s);
        } else {
            return "Missing string 'messages." + key + "' in config.yml";
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        if(!joinNotificationEnabled) {
            debug("Join notifications are disabled, skipping...");
            return;
        }

        debug("User joined. Checking for updates...");

        if(!isPluginEnabled()) {
            debug("Cannot notify user. Plugin is disabled.");
            return;
        }

        final Player p = e.getPlayer();

        new LoginRunner(this, p.getUniqueId()).runTaskLaterAsynchronously(this, joinNotificationDelay * 20);
    }

    @EventHandler
    public void onChestShopTransaction(TransactionEvent e) {
        if (e.getStock().length == 0) return;

        UUID ownerId = e.getOwnerAccount().getUuid();

        if(!this.logAdminShop && NameManager.isAdminShop(ownerId)) return;

        BigDecimal price = e.getExactPrice();
        UUID clientId = e.getClient().getUniqueId();

        String itemId = "";
        int itemQuantities = 0;
        Material itemType = null;

        for (ItemStack item : e.getStock()) {
            if (itemType == null) {
                itemType = item.getType();
                itemId = ItemUtil.getName(item);
            }
            if (item.getType() == itemType) {
                itemQuantities += item.getAmount();
            } else {
                getLogger().log(Level.WARNING, "Transaction event with multiple different item types are not supported in this version of the plugin! Please look for an update. Only logging the first item.");
            }
        }

        // Check whether we need to start a new runner by checking if the batch is empty
        boolean startRunner = batch.isEmpty();

        batch.add(new HistoryEntry(
                ownerId,
                clientId,
                e.getClient().getName(),
                itemId,
                price.doubleValue(),
                Time.getEpochTime(),
                e.getTransactionType(),
                itemQuantities,
                true
        ));

        debug("Item added to batch.");

        if (startRunner) {
            new BatchRunner(this).runTaskAsynchronously(this);
        }
    }

    public void debug(String d) {
        if(verboseEnabled)
            getLogger().log(Level.INFO, d);
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();

    }

    public Queue<HistoryEntry> getBatch() {
        return batch;
    }

    public String getPlayerName(UUID playerId, String playerName) {
        try {
            return playerNames.get(playerId, () -> {
                Player player = getServer().getPlayer(playerId);
                if (player != null) {
                    return player.getName();
                } else {
                    Account account = NameManager.getAccount(playerId);
                    if (account != null && account.getName() != null) {
                        return account.getName();
                    }
                    if (playerName == null) {
                        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(playerId);
                        if (offlinePlayer != null && offlinePlayer.getName() != null) {
                            return offlinePlayer.getName();
                        }
                    } else {
                        return playerName;
                    }
                }
                throw new Exception("Player not found");
            });
        } catch (ExecutionException e) {
            return playerName;
        }
    }

    private enum DbType {
        SQLITE,
        MYSQL;
    }
}
