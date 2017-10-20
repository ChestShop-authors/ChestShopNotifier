package com.wfector.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.wfector.notifier.ChestShopNotifier;
import com.wfector.util.Time;
import com.Acrobot.ChestShop.Economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class History extends BukkitRunnable {

    private final UUID userId;
    private final CommandSender sender;
    private final boolean markRead;
    private int page;
    private final int maxRows;

    private final List<HistoryEntry> historyEntries = new ArrayList<>();

    private ChestShopNotifier plugin;

    public History(ChestShopNotifier plugin, UUID userId, CommandSender sender, int page, boolean markRead) {
        this.plugin = plugin;
        this.userId = userId;
        this.sender = sender;
        this.page = page > 0 ? page : 1;
        this.markRead = markRead;
        maxRows = plugin.getConfig().getInt("history.max-rows");
    }


    public void run() {
        gatherResults();
        showResults();
    }

    private void gatherResults() {
        Connection c = null;
        try {
            c = plugin.getConnection();
            Statement statement = c.createStatement();

            ResultSet res = statement.executeQuery("SELECT * FROM `csnUUID` WHERE `ShopOwnerId`='" + userId.toString() + "' ORDER BY `Id` DESC LIMIT 1000;");

            while (res.next()) {
                HistoryEntry entry = new HistoryEntry(
                        UUID.fromString(res.getString("CustomerId")),
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
        } finally {
            ChestShopNotifier.close(c);
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
            String userName = NameManager.getUsername(userId);
            message += ChatColor.GRAY + " (" + (userName != null ? userName : userId) + ")";
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
            String msgString = plugin.getMessage("history-" + (entry.getType() == TransactionType.BUY ? "bought" : "sold") + (entry.isUnread() ? "" : "-read"));

            String playerName = NameManager.getUsername(entry.getCustomerId());

            msgString = msgString.replace("{player}", playerName != null ? playerName : "unknown");
            msgString = msgString.replace("{count}", String.valueOf(entry.getQuantity()));
            msgString = msgString.replace("{item}", entry.getItemId().replace(" ", ""));
            msgString = msgString.replace("{timeago}", Time.getAgo(entry.getTime()));
            msgString = msgString.replace("{money}", Economy.formatBalance(entry.getPrice() * entry.getQuantity()));

            sender.sendMessage(msgString);
        }

        sender.sendMessage(" ");
        if (maxPages > 1) {
            sender.sendMessage(
                    plugin.getMessage("history-footer-page")
                            .replace("{current}", String.valueOf(page))
                            .replace("{pages}", String.valueOf(maxPages))
            );
        }
        if (!other && sender.hasPermission("csn.command.clear")) {
            sender.sendMessage(plugin.getMessage("history-footer-clear"));
        }

    }

    private class HistoryEntry {
        private final UUID customerId;
        private final String itemId;
        private double price;
        private int time;
        private final TransactionType type;
        private int quantity;
        private final boolean unread;

        public HistoryEntry(UUID customerId, String itemId, double amount, int time, TransactionType type, int quantity, boolean unread) {
            this.customerId = customerId;
            this.itemId = itemId;
            this.price = amount / quantity;
            this.time = time;
            this.type = type;
            this.quantity = quantity;
            this.unread = unread;
        }

        public UUID getCustomerId() {
            return customerId;
        }

        public String getItemId() {
            return itemId;
        }

        public double getPrice() {
            return price;
        }

        public int getTime() {
            return time;
        }

        public TransactionType getType() {
            return type;
        }

        public int getQuantity() {
            return quantity;
        }

        public boolean isUnread() {
            return unread;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof HistoryEntry) {
                HistoryEntry entry = (HistoryEntry) o;
                return this.getCustomerId().equals(entry.getCustomerId())
                        && this.getItemId().equals(entry.getItemId())
                        && this.getPrice() == entry.getPrice()
                        && this.getTime() == entry.getTime()
                        && this.getType() == entry.getType()
                        && this.getQuantity() == entry.getQuantity()
                        && this.isUnread() == entry.isUnread();
            }
            return false;
        }

        public boolean isSimilar(HistoryEntry entry) {
            return equals(entry)
                    || this.getCustomerId().equals(entry.getCustomerId())
                    && this.getType() == entry.getType()
                    && this.getPrice() == entry.getPrice()
                    && this.getItemId().equals(entry.getItemId())
                    && this.getTime() < entry.getTime() + 5 * 60 // Check if they are a maximum of 5 minutes apart
                    && this.getTime() > entry.getTime() - 5 * 60;
        }

        public void mergeWith(HistoryEntry entry) {
            quantity += entry.getQuantity();
            if (time < entry.getTime()) {
                time = entry.getTime();
            }
        }
    }
}
