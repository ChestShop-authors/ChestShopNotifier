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
import static com.Acrobot.Breeze.Utils.MaterialUtil.getSignName;

public class ChestShopNotifier extends JavaPlugin implements Listener {
	
	public MySQL MySQL;
	Connection c = null;

	private FileConfiguration config;
	private ArrayList<String> batch = new ArrayList<String>();
	
	private boolean verboseEnabled;
	private boolean joinNotificationEnabled;
	private Integer joinNotificationDelay;
	
	private String dbHost;
	private Integer dbPort;
	private String dbName;
	private String dbUsername;
	private String dbPassword;
	
	private Connection database;
	
	public boolean pluginEnabled = false;
	public boolean newNotifications = false;
	public boolean logAdminShop = true;
	
	ChestShopNotifier plugin = this;
	Integer theAmount = 0;
	ArrayList<UUID> notifyusers_ids = new ArrayList<UUID>();
	ArrayList<Integer> notifyusers_sales = new ArrayList<Integer>();
	ArrayList<Integer> notifyusers_times = new ArrayList<Integer>();
	
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
		
		this.getLogger().log(Level.INFO, "Connected!");
		
		try {
			Statement statement = c.createStatement();
			
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS csnUUID (Id int(11) AUTO_INCREMENT, ShopOwnerId VARCHAR(36), CustomerId VARCHAR(36), ItemId VARCHAR(1000), Mode INT(11), Amount FLOAT(53), Quantity INT(11), Time INT(11), Unread INT(11), PRIMARY KEY (Id))");
			
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			this.setEnabled(false);
			return;
		}
		
		if(this.isEnabled() && pluginEnabled) {
		    		    
		    Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

				@Override
				public void run() {
					try {
						runNotifier();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} 
			}, 60L, 60L);
		    
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

	public boolean updateConfiguration(boolean isReload) {
		if(isReload) this.reloadConfig();
		
		this.config = this.getConfig();
		
		verboseEnabled = this.config.getBoolean("debugging.verbose");
		joinNotificationEnabled = this.config.getBoolean("notifications.notify-on-user-join");
		joinNotificationDelay = this.config.getInt("notifications.delay-seconds");
		
		this.dbHost = this.config.getString("database.host");
		this.dbPort = this.config.getInt("database.port");
		this.dbName = this.config.getString("database.dbname");
		this.dbUsername = this.config.getString("database.username");
		this.dbPassword = this.config.getString("database.password");
		
		this.logAdminShop = this.config.getBoolean("logging.admin-shop");		
				
		if(isReload) { 
			MySQL = new MySQL(this, dbHost, dbPort.toString(), dbName, dbUsername, dbPassword);
			
			this.getLogger().log(Level.INFO, "Connecting to the database...");
			
			c = MySQL.openConnection();
			
			if(c == null) {
				this.getLogger().log(Level.WARNING, "Failed to connect to the database! Disabling connections!");
				
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
		return (this.getConfig().contains("messages." + string)) ? ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messages." + string)) : null;
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
		if(joinNotificationEnabled == false) {
			debug("Join notifications are " + joinNotificationEnabled + ", skipping...");
			return;
		}
		
		debug("User joined. Checking for updates...");
		
		final Player p = e.getPlayer();
		final UUID pId = p.getUniqueId();
		this.theAmount = 0;

		if(!pluginEnabled) {
			debug("Cannot notify user. Plugin is disabled.");
			return;
		}
		
		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

			@Override
			public void run() {
				Connection batchConnection;
				if(!connect()) return;
				batchConnection = plugin.database;
				
				Statement statement = null;
				try {
					statement = batchConnection.createStatement();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				ResultSet res = null;
				try {
					res = statement.executeQuery("SELECT `ShopOwnerId` FROM csnUUID WHERE `ShopOwnerId`='" + pId.toString() + "' AND `Unread`='0'"); 
					
					res.next();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				Integer amount = 0;
				try {
					if(res.getMetaData().getColumnCount() > 0) 
						while(res.next()) 
							amount++;
					
					debug("Found rows: " + amount.toString());
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				if(p.isOnline()) {
					if(amount > 0) 
						plugin.theAmount = amount;
				}
				
				try {
					batchConnection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				if(theAmount > 0 && p.isOnline()) {
					Date dt = new Date();
					debug("Added message to queue (delay s: " + joinNotificationDelay + ")");
					Integer SendTime = (int) (dt.getTime() / 1000) + joinNotificationDelay;
					
					plugin.notifyusers_ids.add(pId);
					plugin.notifyusers_sales.add(theAmount);
					plugin.notifyusers_times.add(SendTime);
					plugin.newNotifications = true;
				}
			}
			
		});
		
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
        
        try {
			this.runBatch();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		return true;
	}
		
	public boolean connect() {
	    try {
	    	this.database = MySQL.openConnection();
	    } catch (Exception e) {
	    	this.getLogger().warning("Could not establish database connection!");
	    	return false;
	    }
		return true;
	}
	
	public void runNotifier() throws SQLException {
		
		if(!newNotifications) return;
		if(!pluginEnabled) return;
		
		for(int i = 0; i < notifyusers_ids.size(); i++) {
			Integer sales = notifyusers_sales.get(i);
			UUID userid = notifyusers_ids.get(i);
			
			Player p = Bukkit.getPlayer(userid);
			if(p != null) {
				debug("Ran for user '" + p.getName() + "'");
				if(plugin.getMessage("sales") != null) p.sendMessage(this.getMessage("sales").replace("{sales}", sales.toString()));
				if(plugin.getMessage("history-cmd") != null) p.sendMessage(this.getMessage("history-cmd"));
			} else {
				debug("Warning: The player with the uuid '" + userid + "' could not be found, yet was in queue.");
			}
		}

		debug("Finished.");
		notifyusers_ids.clear();
		notifyusers_sales.clear();
		
		newNotifications = false;
	}


	public void runBatch() throws SQLException {
		
		debug("Uploading a batch...");
		
		if(batch.isEmpty()) return;
		if(!pluginEnabled) return;
		if(!connect()) return;
		
		if(batch.size() > 0) {
			
			Connection batchConnection = this.database;
			
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
	
}
