package com.wfector.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import com.wfector.notifier.HistoryEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.wfector.notifier.ChestShopNotifier;
import com.wfector.util.Time;
import com.Acrobot.ChestShop.Economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class History extends BukkitRunnable {

    private final UUID userId;
    private final String userName;
    private final CommandSender sender;
    private final boolean markRead;
    private int page;
    private final int maxRows;
    private int queryLimit;

    private final List<HistoryEntry> historyEntries = new ArrayList<>();

    private ChestShopNotifier plugin;

    public History(ChestShopNotifier plugin, UUID userId, String userName, CommandSender sender, int page, boolean markRead) {
        this.plugin = plugin;
        this.userId = userId;
        this.userName = userName;
        this.sender = sender;
        this.page = page > 0 ? page : 1;
        this.markRead = markRead;
        maxRows = plugin.getConfig().getInt("history.max-rows");
        queryLimit = plugin.getConfig().getInt("history.query-limit");
        if (queryLimit <= 0) {
            queryLimit = 10000;
        }
    }


    public void run() {
        gatherResults();
        plugin.getServer().getScheduler().runTask(plugin, this::showResults);
    }

    private void gatherResults() {
        try (Connection c = plugin.getConnection()){
            Statement statement = c.createStatement();

            ResultSet res = statement.executeQuery("SELECT * FROM `csnUUID` WHERE `ShopOwnerId`='" + userId.toString() + "' ORDER BY `Id` DESC LIMIT " + queryLimit + ";");

            while (res.next()) {
                UUID customerId = UUID.fromString(res.getString("CustomerId"));
                String customerName = plugin.getPlayerName(customerId, res.getString("CustomerName"));
                HistoryEntry entry = new HistoryEntry(
                        userId,
                        customerId,
                        customerName,
                        res.getString("ItemId"),
                        res.getDouble("Amount"),
                        res.getInt("Time"),
                        res.getInt("Mode") == 1 ? TransactionType.BUY : TransactionType.SELL,
                        res.getInt("Quantity"),
                        res.getInt("Unread") == 0
                );
                addEntry(entry);
            }

            res.close();

            if (markRead) {
                Statement readStatement = c.createStatement();
                int rowsUpdated = readStatement.executeUpdate("UPDATE csnUUID SET `Unread`='1' WHERE `ShopOwnerId`='" + userId.toString() + "'");
                if (rowsUpdated > 0)
                sender.sendMessage(plugin.getMessage("history-marked-read"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(HistoryEntry addEntry) {
        for (HistoryEntry entry : historyEntries) {
            if (addEntry.isSimilar(entry)) {
                entry.mergeWith(addEntry);
                return;
            }
        }

        historyEntries.add(addEntry);
    }

    private void showResults() {
        boolean other = !(sender instanceof Player) || !((Player) sender).getUniqueId().equals(userId);
        String message = plugin.getMessage("history-caption");
        if (other) {
            message += ChatColor.GRAY + " (" + userName + ")";
        }
        sender.sendMessage(message);
        sender.sendMessage("");

        if(historyEntries.isEmpty()) {
            sender.sendMessage(plugin.getMessage("history-empty"));
            return;
        }

        int maxPages = (int) Math.ceil(historyEntries.size() / maxRows);
        if (maxPages > 0 && page > maxPages) {
            page = maxPages;
        }

        for(int i = maxRows * (page - 1); i < historyEntries.size() && i < maxRows * page; i++) {
            HistoryEntry entry = historyEntries.get(i);
            String msgString = plugin.getMessage("history-" + (entry.getType() == TransactionType.BUY ? "bought" : "sold") + (entry.isUnread() ? "" : "-read"),
                    "player", entry.getCustomerName() != null ? entry.getCustomerName() : "unknown",
                    "count", String.valueOf(entry.getQuantity()),
                    "item", entry.getItemId().replace(" ", ""),
                    "timeago", Time.getAgo(entry.getTime()),
                    "money", Economy.formatBalance(entry.getAmountPaid())
            );

            sender.sendMessage(msgString);
        }

        sender.sendMessage(" ");
        if (maxPages > 1) {
            sender.sendMessage(
                    plugin.getMessage("history-footer-page",
                            "current", String.valueOf(page),
                            "pages", String.valueOf(maxPages))
            );
        }
        if (!other && sender.hasPermission("csn.command.clear")) {
            sender.sendMessage(plugin.getMessage("history-footer-clear"));
        }

    }

}
