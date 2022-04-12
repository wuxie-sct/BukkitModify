package me.wuxie.bukkitmodify.event;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class InventoryAddItemPostEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Getter
    private ItemStack[] itemStacks;
    @Getter
    private final Inventory inventory;
    @Getter
    private final Map<Integer, ItemStack> leftover;

    public InventoryAddItemPostEvent(Inventory inventory, ItemStack[] itemStacks, Map<Integer, ItemStack> leftover) {
        this.inventory = inventory;
        this.itemStacks = itemStacks;
        this.leftover = leftover;
    }

    public void setItemStacks(ItemStack[] itemStacks) {
        if (itemStacks == null) {
            itemStacks = new ItemStack[0];
        }
        this.itemStacks = itemStacks;
    }

    public static InventoryAddItemPostEvent callEvent(Inventory inventory, ItemStack[] itemStacks, Map<Integer, ItemStack> leftover) {
        InventoryAddItemPostEvent event = new InventoryAddItemPostEvent(inventory, itemStacks, leftover);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }
}