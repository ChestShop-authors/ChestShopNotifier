package com.wfector.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import code.husky.mysql.MySQL;

public class Clear {

	public static void ClearHistory(MySQL m, String userName) {
		Connection c = m.openConnection();
		
		try {
			Statement statement = c.createStatement();
			statement.executeUpdate("UPDATE csn SET `Unread`='1' WHERE `ShopOwner`='" + userName + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
}
