package com.wfector.notifier;

import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

public class Login
  extends BukkitRunnable
{
  private Main plugin;
  
  public Login(Main main)
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