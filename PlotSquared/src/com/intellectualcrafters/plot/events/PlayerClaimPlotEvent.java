/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = PlayerClaimPlotEvent.java
 * >> Generated by: Citymonstret at 2014-08-09 15:21
 */

package com.intellectualcrafters.plot.events;

import com.intellectualcrafters.plot.Plot;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Created by Citymonstret on 2014-08-09.
 */
public class PlayerClaimPlotEvent extends PlayerEvent implements Cancellable{
    private static HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private Plot plot;

    public PlayerClaimPlotEvent(Player player, Plot plot) {
        super(player);
        this.plot = plot;
    }
    
    public Plot getPlot() {
        return this.plot;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}