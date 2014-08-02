package com.wfector.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import code.husky.mysql.MySQL;

public class Clear {

	public static void ClearHistory(MySQL m, UUID userId) {
		Connection c = m.openConnection();
		
		try {
			Statement statement = c.createStatement();
			statement.executeUpdate("UPDATE csnUUID SET `Unread`='1' WHERE `ShopOwnerId`='" + userId.toString() + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
}
