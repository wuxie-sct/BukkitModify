package me.wuxie.bukkitmodify.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventorySetItemEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Getter
    private final Inventory inventory;
    @Getter
    @Setter
    private int slot;
    @Getter
    @Setter
    ItemStack itemStack;
    public InventorySetItemEvent(Inventory inventory,int slot, ItemStack itemStack){
        this.inventory = inventory;
        this.slot = slot;
        this.itemStack = itemStack;
    }
    public static InventorySetItemEvent callEvent(Inventory inventory,int slot,ItemStack itemStack){
        InventorySetItemEvent event = new InventorySetItemEvent(inventory,slot,itemStack);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }
}
