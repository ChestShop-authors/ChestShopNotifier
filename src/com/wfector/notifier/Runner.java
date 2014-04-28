package com.wfector.notifier;

import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

public class Runner
  extends BukkitRunnable
{
  private Main plugin;
  
  public Runner(Main main)
  {
    this.plugin = main;
  }
  
  public void run()
  {
    try {
		this.plugin.runBatch();
	} catch (SQLException e) {
		e.printStackTrace();
	}
  }
}
