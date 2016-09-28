package com.wfector.notifier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
    Connection c = null;

    private ArrayList<String> batch = new ArrayList<String>();

    private boolean verboseEnabled;
    private boolean joinNotificationEnabled;
    private Integer joinNotificationDelay;

    private Connection conn;

    public boolean pluginEnabled = false;
    public boolean logAdminShop = true;

    ChestShopNotifier plugin = this;

    private Notifier notifier;

    public void onEnable() {
        this.saveDefaultConfig();
        updateConfiguration(true);

        this.getLogger().log(Level.INFO, "Connecting to the database...");

        c = MySQL.openConnection();

        if(c == null) {
            this.getLogger().log(Level.WARNING, "Failed to connect to the database! Disabling connections!");

            return;
        }

        pluginEnabled = true;

        getLogger().log(Level.INFO, "Connected!");

        try {
            Statement statement = c.createStatement();

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS csnUUID (Id int(11) AUTO_INCREMENT, ShopOwnerId VARCHAR(36), CustomerId VARCHAR(36), ItemId VARCHAR(1000), Mode INT(11), Amount FLOAT(53), Quantity INT(11), Time INT(11), Unread INT(11), PRIMARY KEY (Id))");

        } catch (SQLException e) {
            e.printStackTrace();

            setEnabled(false);
            return;
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if(isPluginEnabled()) {
            notifier = new Notifier(this);
            notifier.runTaskTimer(this, 60, 60);

            getServer().getPluginManager().registerEvents(this, this);
        }
    }

    public void onDisable() {
        if(batch.size() > 0) {
            this.getLogger().log(Level.INFO, "Database queue is not empty. Uploading now...");
            try {
                runBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            this.getLogger().log(Level.INFO, "Done uploading database queue!");
        }

    }

    public boolean isPluginEnabled() {
        return isEnabled() && pluginEnabled;
    }

    public boolean updateConfiguration(boolean isReload) {
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

            c = MySQL.openConnection();

            if(c == null) {
                getLogger().log(Level.WARNING, "Failed to connect to the database! Disabling connections!");

                pluginEnabled = false;
                return false;
            }
            pluginEnabled = true;
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("csn")) {
            CommandRunner c = new CommandRunner();
            c.SetPlugin(this);
            c.Process(sender, cmd, label, args);

            return true;
        }
        return false;
    }


    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        if(joinNotificationEnabled) {
            debug("Join notifications are " + joinNotificationEnabled + ", skipping...");
            return;
        }

        debug("User joined. Checking for updates...");

        final Player p = e.getPlayer();
        final UUID pId = p.getUniqueId();

        if(!isPluginEnabled()) {
            debug("Cannot notify user. Plugin is disabled.");
            return;
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                Connection batchConnection;
                if (!connect()) return;
                batchConnection = plugin.getConnection();

                Statement statement;
                ResultSet res;
                try {
                    statement = batchConnection.createStatement();

                    res = statement.executeQuery("SELECT `ShopOwnerId` FROM csnUUID WHERE `ShopOwnerId`='" + pId.toString() + "' AND `Unread`='0'");

                    res.next();

                    int amount = 0;
                    if (res.getMetaData().getColumnCount() > 0)
                        while (res.next())
                            amount++;

                    debug("Found rows: " + String.valueOf(amount));

                    if (amount > 0 && p.isOnline()) {
                        Date dt = new Date();
                        debug("Added message to queue (delay s: " + joinNotificationDelay + ")");
                        int sendTime = (int) (dt.getTime() / 1000) + joinNotificationDelay;

                        plugin.getNotifier().add(pId, amount, sendTime);
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        batchConnection.close();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            }

        }.runTaskAsynchronously(this);

        debug("Done.");
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
        Integer itemQuantitys = 0;

        for (ItemStack item : e.getStock()) {
            items.append(getSignName(item));
            itemQuantitys = item.getAmount();
        }

        String itemId = items.toString();
        Integer itemQuant = itemQuantitys;

        batch.add("('" + ownerId.toString() + "', '" + clientId.toString() + "', '" + itemId + "', '" + mode.toString() + "', '" + String.valueOf(price) + "', '" + Time.GetEpochTime() + "', '" + itemQuant.toString() + "', '0')");

        debug("Item added to batch.");
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                try {
                    runBatch();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        return true;
    }

    public boolean connect() {
        try {
            this.conn = MySQL.openConnection();
        } catch (Exception e) {
            this.getLogger().warning("Could not establish database connection!");
            return false;
        }
        return true;
    }


    public void runBatch() throws SQLException {

        debug("Uploading a batch...");

        if(batch.isEmpty()) return;
        if(!pluginEnabled) return;
        if(!connect()) return;

        if(batch.size() > 0) {

            Connection batchConnection = this.conn;

            String qstr = "INSERT INTO csnUUID (`ShopOwnerId`, `CustomerId`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES ";

            int i = 0;

            for(String query : batch) {
                qstr += query;
                if(batch.size() > (i+1)) {
                    qstr += ", ";
                }
                i++;
            }

            Statement statement = batchConnection.createStatement();
            statement.executeUpdate(qstr);
            debug("Update: " + qstr);

            batch.clear();

            batchConnection.close();
        }

        debug("Batch completed.");
    }

    public void debug(String d) {
        if(verboseEnabled)
            this.getLogger().log(Level.INFO, d);
    }

    public Connection getConnection() {
        if(!this.connect()) return null;
        return this.conn;

    }

    public Notifier getNotifier() {
        return notifier;
    }
}
