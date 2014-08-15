package com.wfector.notifier;

import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

public class Login
  extends BukkitRunnable
{
  private ChestShopNotifier plugin;
  
  public Login(ChestShopNotifier main)
  {
    this.plugin = main;
  }
  
  public void run()
  {
    try {
		this.plugin.runNotifier();
	} catch (SQLException e) {
		e.printStackTrace();
	}
  }
}