# ChestShopNotifier

CSN (ChestShop Notifier) is a plugin that records ChestShop transactions to a MySQL database. When a shop owner logs onto the server, they will be presented with an option to view sales from when they were offline. 

##Downloads

You can download release versions of the plugin from the [ChestShopNotifier Bukkit dev page](https://dev.bukkit.org/bukkit-plugins/csn/)!

Development builds can be found on the [Minebench.de](https://www.minebench.de/) Jenkins build server: http://ci.minebench.de/job/ChestShopNotifier/

##Installation Instructions

Installing this plugin is simple. Just download and drag ChestShopNotifier.jar into your server's *plugins* directory. 

Next, restart and log into your server. When you try to use */csn history*, you'll receive an error that the database isn't connected. Follow these steps to get it working: 

1. Open the plugin folder (/plugins/ChestShopNotifier)
2. Edit the config.yml with your database settings (feel free to change other config as well)
3. Save the file
4. In-game, run */csn reload* to reload config and connect to the database. 

If it says the database was connected, you're good to go!

##Commands

- /csn help → Shows available commands
- /csn history [page] → Shows sale history and marks them as read
- /csn history [username [page]] → Shows sale history of another player
- /csn clear → Removes as read entries (from the database)
- /csn cleandatabase → Remove database entries. Parameters:    
  --older-than, -o days → Removes entries older than days    
  --user, -user username/uuid → Removes entries from a single user only    
  --read-only, -r → Removes only read entries, default behaviour    
  --all, -a → Removes all entries
- /csn upload → Forces the plugin to upload all data to the database (in v1.1 and higher this should be only used after database connection problems!)
- /csn reload → Reloads configuration and connects to database using new config
- /csn convert → Converts the database table csn to UUIDs. (table csn gets converted in csnUUID and the username table renamed to csnOLD) 

##Permissions

- csn.user → Allows user to receive notifications when they log in, and access to /csn history.
- csn.admin → Super permission node. Access to all commands. (especially upload, reload and convert) 

For all permissions take a look at the [plugin.yml](https://github.com/Phoenix616/ChestShopNotifier/blob/master/src/main/resources/plugin.yml#L14).

##Requirements

- Functional MySQL server. This plugin only uses MySQL, nothing else.
- ChestShop installed and running. 

##Planned Features

To request features open an [issue](https://github.com/Phoenix616/ChestShopNotifier/issues)!

- [x] ~~UUIDsupport~~ *done in v1.1.2*
- [x] ~~Look up the sales of offline players~~ *done in v1.1.2*
- [x] ~~Ignore ChestShop's Admin Shop~~ *done in v1.1.2*
- [x] ~~History pagination~~ *done in v1.2*
- [ ] Auto updater & version checker
- [ ] Translation system *(partially done in v1.1.2, you can set your own messages in the config)*

For all changes take a look at the [commits](https://github.com/Phoenix616/ChestShopNotifier/commits/master)!

##Demonstration

![Preview 1](http://dev.bukkit.org/media/images/73/384/Preview1.jpg)

![Preview 2](http://dev.bukkit.org/media/images/73/385/Preview2.jpg)
