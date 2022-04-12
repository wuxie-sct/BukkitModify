package me.wuxie.bukkitmodify.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryAddItemPreEvent extends Event implements Cancellable {
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
    @Setter
    private boolean cancelled;

    public InventoryAddItemPreEvent(Inventory inventory,ItemStack[] itemStacks){
        this.inventory = inventory;
        this.itemStacks = itemStacks;

    }

    public void setItemStacks(ItemStack[] itemStacks) {
        if(itemStacks==null){
            itemStacks = new ItemStack[0];
        }
        this.itemStacks = itemStacks;
    }
    public static InventoryAddItemPreEvent callEvent(Inventory inventory,ItemStack[] itemStacks){
        InventoryAddItemPreEvent event = new InventoryAddItemPreEvent(inventory,itemStacks);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }
}