package com.wfector.notifier;

import com.Acrobot.ChestShop.Events.TransactionEvent;

import java.util.UUID;

public class HistoryEntry {
    private final UUID shopOwnerId;
    private final UUID customerId;
    private final String customerName;
    private final String itemId;
    private double amountPaid;
    private int time;
    private final TransactionEvent.TransactionType type;
    private int quantity;
    private final boolean unread;

    public HistoryEntry(UUID shopOwnerId, UUID customerId, String customerName, String itemId, double amountPaid, int time, TransactionEvent.TransactionType type, int quantity, boolean unread) {
        this.shopOwnerId = shopOwnerId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.itemId = itemId;
        this.amountPaid = amountPaid;
        this.time = time;
        this.type = type;
        this.quantity = quantity;
        this.unread = unread;
    }

    public UUID getShopOwnerId() {
        return shopOwnerId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getItemId() {
        return itemId;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public double getPricePerItem() {
        return amountPaid / quantity;
    }

    public int getTime() {
        return time;
    }

    public TransactionEvent.TransactionType getType() {
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
                    && this.getAmountPaid() == entry.getAmountPaid()
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
                && this.getPricePerItem() == entry.getPricePerItem()
                && this.getItemId().equals(entry.getItemId())
                && this.getTime() < entry.getTime() + 5 * 60 // Check if they are a maximum of 5 minutes apart
                && this.getTime() > entry.getTime() - 5 * 60;
    }

    public void mergeWith(HistoryEntry entry) {
        quantity += entry.getQuantity();
        amountPaid += entry.getAmountPaid();
        if (time < entry.getTime()) {
            time = entry.getTime();
        }
    }
}
