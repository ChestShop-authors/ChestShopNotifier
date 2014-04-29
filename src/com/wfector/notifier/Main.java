package com.wfector.notifier;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import com.wfector.command.Clear;
import com.wfector.command.CommandRunner;
import com.wfector.command.Help;
import com.wfector.command.History;
import com.wfector.util.Time;

import code.husky.mysql.MySQL;
import static com.Acrobot.Breeze.Utils.MaterialUtil.getSignName;

public class Main extends JavaPlugin implements Listener {
	
	public MySQL MySQL;
	Connection c = null;

	private FileConfiguration config;
	private Runner runner;
	private Login login;
	private ArrayList<String> batch = new ArrayList<String>();
	private ArrayList<String> users = new ArrayList<String>();
	
	private boolean verboseEnabled;
	private boolean joinNotificationEnabled;
	private Integer joinNotificationDelay;
	
	private String dbHost;
	private Integer dbPort;
	private String dbName;
	private String dbUsername;
	private String dbPassword;
	
	public boolean pluginEnabled = false;
	
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
			
			int res = statement.executeUpdate("CREATE TABLE IF NOT EXISTS csn (Id int(11) AUTO_INCREMENT, ShopOwner VARCHAR(1000), Customer VARCHAR(1000), ItemId VARCHAR(1000), Mode INT(11), Amount INT(11), Quantity INT(11), Time INT(11), PRIMARY KEY (Id))");
			
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			this.setEnabled(false);
			return;
		}
		
		if(this.isEnabled() && pluginEnabled) {
		    this.runner = new Runner(this);
		    this.runner.runTaskTimer(this, 2000L, 2000L);
		    
		    this.login = new Login(this);
		    this.login.runTaskTimerAsynchronously(this, 5L, 5L);
		    
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
	ArrayList<String> notifyusers_names = new ArrayList<String>();
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
		final String pName = p.getName();
		this.theAmount = 0;

		if(!pluginEnabled) return;
		
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
					res = statement.executeQuery("SELECT `ShopOwner` FROM csn WHERE `ShopOwner`='" + pName + "' AND `Unread`='0'");
					
					res.next();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				Integer amount = 0;
				try {
					if(res.getMetaData().getColumnCount() > 0)
					while(res.next()) {
						amount++;
					}
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
					Integer SendTime = (int) (dt.getTime() / 1000) + joinNotificationDelay;
					
					plugin.notifyusers_names.add(pName);
					plugin.notifyusers_sales.add(theAmount);
					plugin.notifyusers_times.add(SendTime);
				}
			}
			
		});
		
		debug("Done.");
	}
	
	@EventHandler
	public boolean onChestShopTransaction(TransactionEvent e) {
		String ownerName = e.getOwner().getName();
		TransactionType f = e.getTransactionType();
		
		Integer mode = 0;
		
		if(f == TransactionType.BUY) { mode = 1; }
		else { mode = 2; }
		
		Integer price = (int) e.getPrice();
		String clientName = e.getClient().getName();

		StringBuilder items = new StringBuilder(50);
		Integer itemQuantitys = 0;

        for (ItemStack item : e.getStock()) {
            items.append(getSignName(item));
            itemQuantitys = item.getAmount();
        }
        
        String itemId = items.toString();
        Integer itemQuant = itemQuantitys;
        
        batch.add("('" + ownerName + "', '" + clientName + "', '" + itemId + "', '" + mode.toString() + "', '" + price.toString() + "', '" + Time.GetEpochTime() + "', '" + itemQuant.toString() + "', '0')");
        
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
		
		if(notifyusers_sales.isEmpty()) return;
		if(!pluginEnabled) return;
		
		int i = 0;
		
		Integer sales = notifyusers_sales.get(i);
		String username = notifyusers_names.get(i);
		Integer time = notifyusers_times.get(i);

		Date dt = new Date();
		Integer CurrentTime = (int) (dt.getTime() / 1000);
			
		if(time < CurrentTime) {
			Player p = Bukkit.getPlayer(username);
			if(p != null) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[CSN] &cYou received &a" + sales.toString() + "&c sales since you last checked. To see them, type /csn history."));
			}
			notifyusers_sales.clear();
			notifyusers_times.clear();
			notifyusers_names.clear();
		}
	}
	
	public void runBatch() throws SQLException {
		
		debug("Running a batch...");
		
		if(batch.isEmpty()) return;
		if(!pluginEnabled) return;
		if(!connect()) return;
		
		if(batch.size() > 0) {
			
			Connection batchConnection = this.database;
			
			String qstr = "INSERT INTO csn (`ShopOwner`, `Customer`, `ItemId`, `Mode`, `Amount`, `Time`, `Quantity`, `Unread`) VALUES ";
			
			int i = 0;
			
			for(String query : batch) {
				qstr += query;
				if(batch.size() > (i+1)) {
					qstr += ", ";
				}
				
				i++;
				
			}
			
			Statement statement = batchConnection.createStatement();
			int res = statement.executeUpdate(qstr);
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
