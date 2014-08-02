package com.wfector.notifier;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import com.wfector.command.CommandRunner;
import com.wfector.util.Time;

import code.husky.mysql.MySQL;
import static com.Acrobot.Breeze.Utils.MaterialUtil.getSignName;

public class Main extends JavaPlugin implements Listener {
	
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
	
	public boolean pluginEnabled = false;
	public boolean newNotifications = false;
	
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	
	public boolean updateConfiguration(boolean isReload) {
		if(isReload) reloadCustomConfig();
		
		this.config = getCustomConfig();
		
		verboseEnabled = this.config.getBoolean("debugging.verbose");
		joinNotificationEnabled = this.config.getBoolean("notifications.notify-on-user-join");
		joinNotificationDelay = this.config.getInt("notifications.delay-seconds");
		
		dbHost = this.config.getString("database.host");
		dbPort = this.config.getInt("database.port");
		dbName = this.config.getString("database.dbname");
		dbUsername = this.config.getString("database.username");
		dbPassword = this.config.getString("database.password");
				
		if(isReload) { 
			MySQL = new MySQL(this, dbHost, dbPort.toString(), dbName, dbUsername, dbPassword);
			
			System.out.println("Connecting to the database...");
			
			c = MySQL.openConnection();
			
			if(c == null) {
				System.out.println("Failed to connect to the database! Disabling connections!");
				
				pluginEnabled = false;
				return false;
			}
			
			pluginEnabled = true;
		}
		
		return true;
	}
	
	public void onEnable() {
		saveDefaultConfig();
		updateConfiguration(true);
		
		System.out.println("Connecting to the database...");
		
		c = MySQL.openConnection();
		
		if(c == null) {
			System.out.println("Failed to connect to the database! Disabling connections!");
			
			return;
		}
		
		pluginEnabled = true;
		
		System.out.println("Connected!");
		
		try {
			Statement statement = c.createStatement();
			
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS csnUUID (Id int(11) AUTO_INCREMENT, ShopOwnerId VARCHAR(36), CustomerId VARCHAR(36), ItemId VARCHAR(1000), Mode INT(11), Amount INT(11), Quantity INT(11), Time INT(11), PRIMARY KEY (Id))");
			
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			this.setEnabled(false);
			return;
		}
		
		if(this.isEnabled() && pluginEnabled) {
		    Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {

				@Override
				public void run() {
					try {
						runBatch();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} }, 2000L, 2000L);
		    
		    Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

				@Override
				public void run() {
					try {
						runNotifier();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} }, 60L, 60L);
		    
		    getServer().getPluginManager().registerEvents(this, this);
		}
	}
	public void onDisable() {
		if(batch.size() > 0) {
			System.out.println("Database queue is not empty. Uploading now...");
			try {
				runBatch();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			System.out.println("Done!");
		}
		
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
	
	Main plugin = this;
	Integer theAmount = 0;
	ArrayList<UUID> notifyusers_ids = new ArrayList<UUID>();
	ArrayList<Integer> notifyusers_sales = new ArrayList<Integer>();
	ArrayList<Integer> notifyusers_times = new ArrayList<Integer>();
	
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
					if(res.getMetaData().getColumnCount() > 0) {
					while(res.next()) {
						amount++;
					}
					}
					debug("Found rows: " + amount.toString());
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				if(p.isOnline()) {
					if(amount > 0) {
						plugin.theAmount = amount;
					}
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
		TransactionType f = e.getTransactionType();
		
		Integer mode = 0;
		
		if(f == TransactionType.BUY) { mode = 1; }
		else { mode = 2; }
		
		Integer price = (int) e.getPrice();
		UUID clientId = e.getClient().getUniqueId();

		StringBuilder items = new StringBuilder(50);
		Integer itemQuantitys = 0;

        for (ItemStack item : e.getStock()) {
            items.append(getSignName(item));
            itemQuantitys = item.getAmount();
        }
        
        String itemId = items.toString();
        Integer itemQuant = itemQuantitys;
        
        batch.add("('" + ownerId.toString() + "', '" + clientId.toString() + "', '" + itemId + "', '" + mode.toString() + "', '" + price.toString() + "', '" + Time.GetEpochTime() + "', '" + itemQuant.toString() + "', '0')");
        
        System.out.println("Item added to batch.");
        
		return true;
	}
	
	private Connection database;
	
	public boolean connect() {
	    try
	    {
	    	this.database = MySQL.openConnection();
	    }
	    catch (Exception e) {
	    	return false;
	    }
		return true;
	}
	
	public void runNotifier() throws SQLException {
		
		if(!newNotifications) return;
		if(!pluginEnabled) return;
		
		int i = 0;
		
		for(UUID userid : notifyusers_ids) {
			Integer sales = notifyusers_sales.get(i);
				
			Player p = Bukkit.getPlayer(userid);
			if(p != null) {
				debug("[NotifierQueue] Ran for user '" + p.getName() + "'");
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', 
						"&c ** You made &f" + sales.toString() + " sales&c since you last checked.")
					);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', 
						"&c ** To see them, type &f/csn history&c.")
					);
			}
			else {
				debug("Warning: The player with the uuid '" + userid + "' could not be found, yet was in queue.");
			}
		}

		debug("[NotifierQueue] Finished.");
		notifyusers_ids.clear();
		notifyusers_sales.clear();
		
		newNotifications = false;
	}
	
	public void runBatch() throws SQLException {
		
		debug("Running a batch...");
		
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
			System.out.println("[CSN] Update: " + qstr);
			
			batch.clear();
			
			batchConnection.close();
		}
		else {
			
		}
		
		debug("Batch completed.");
	}
	
	public void debug(String d) {
		if(verboseEnabled) {
			System.out.println(d);
		}
	}
	
	public void reloadCustomConfig() {
	    if (customConfigFile == null) {
	    customConfigFile = new File(getDataFolder(), "config.yml");
	    }
	    customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
	 
	    InputStream defConfigStream = this.getResource("config.yml");
	    if (defConfigStream != null) {
	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        customConfig.setDefaults(defConfig);
	    }
	}
	public FileConfiguration getCustomConfig() {
	    if (customConfig == null) {
	        reloadCustomConfig();
	    }
	    return customConfig;
	}
	public void saveDefaultConfig() {
	    if (customConfigFile == null) {
	        customConfigFile = new File(getDataFolder(), "config.yml");
	    }
	    if (!customConfigFile.exists()) {            
	         plugin.saveResource("config.yml", false);
	     }
	}
}
