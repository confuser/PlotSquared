/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = DBFunc.java
 * >> Generated by: Citymonstret at 2014-08-09 01:43
 */

package com.intellectualcrafters.plot.database;
import com.intellectualcrafters.plot.*;
import com.intellectualcrafters.plot.Logger.LogLevel;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static com.intellectualcrafters.plot.PlotMain.connection;

/**
 * 
 * @author Citymonstret
 *
 */
public class DBFunc {

    /**
     * Set Plot owner
     * @param plot
     * @param uuid
     */
    public static void setOwner(final Plot plot, final UUID uuid) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE `plot` SET `owner` = ? WHERE `plot_id_x` = ? AND `plot_id_z` = ? ");
                    statement.setString(1, uuid.toString());
                    statement.setInt(2, plot.id.x);
                    statement.setInt(3, plot.id.y);
                    statement.executeUpdate();
                    statement.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.DANGER, "Could not set owner for plot " + plot.id);
                }
            }
        });
    }

    /**
     * Create a plot
     * @param plot
     */
    public static void createPlot(Plot plot) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("INSERT INTO `plot`(`plot_id_x`, `plot_id_z`, `owner`, `world`) VALUES(?, ?, ?, ?)");
            stmt.setInt(1, plot.id.x);
            stmt.setInt(2, plot.id.y);
            stmt.setString(3, plot.owner.toString());
            stmt.setString(4, plot.world);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.add(LogLevel.DANGER, "Failed to save plot " + plot.id);
        }
    }

    /**
     * Create tables
     * @throws SQLException
     */
    public static void createTables() throws SQLException{
        Statement stmt = connection.createStatement();
        stmt.addBatch(
            "CREATE TABLE IF NOT EXISTS `plot` (" +
            "`id` int(11) NOT NULL AUTO_INCREMENT," +
            "`plot_id_x` int(11) NOT NULL," +
            "`plot_id_z` int(11) NOT NULL," +
            "`owner` varchar(45) NOT NULL," +
            "`world` varchar(45) NOT NULL," +
            "`timestamp` timestamp not null DEFAULT CURRENT_TIMESTAMP," +
            "PRIMARY KEY (`id`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0");

        stmt.addBatch(
            "CREATE TABLE IF NOT EXISTS `plot_helpers` (" +
            "`plot_plot_id` int(11) NOT NULL," +
            "`user_uuid` varchar(40) NOT NULL" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8"
        );

        stmt.addBatch(
                "CREATE TABLE IF NOT EXISTS `plot_denied` (" +
                "`plot_plot_id` int(11) NOT NULL," +
                "`user_uuid` varchar(40) NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8"
        );

        stmt.addBatch(
            "CREATE TABLE IF NOT EXISTS `plot_settings` (" +
            "  `plot_plot_id` INT(11) NOT NULL," +
            "  `biome` VARCHAR(45) DEFAULT 'FOREST'," +
            "  `rain` INT(1) DEFAULT 0," +
            "  `custom_time` TINYINT(1) DEFAULT '0'," +
            "  `time` INT(11) DEFAULT '8000'," +
            "  `deny_entry` TINYINT(1) DEFAULT '0'," +
            "  `alias` VARCHAR(50) DEFAULT NULL," +
            "  `flags` VARCHAR(512) DEFAULT NULL," +
            "  `position` VARCHAR(50) NOT NULL DEFAULT 'DEFAULT'," +
            "  PRIMARY KEY (`plot_plot_id`)," +
            "  UNIQUE KEY `unique_alias` (`alias`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8"
        );

        stmt.addBatch(
            "ALTER TABLE `plot_settings` ADD CONSTRAINT `plot_settings_ibfk_1` FOREIGN KEY (`plot_plot_id`) REFERENCES `plot` (`id`) ON DELETE CASCADE"
        );

        stmt.executeBatch();
        stmt.clearBatch();
        stmt.close();
        
        /**
         * Adding missing columns (for older versions)
         *  + get current columns (continue if they do not match the current number of columns)
         *  + get data from plot_id column
         *  - create column (plot_id_x,plot_id_z,world)
         *  - populate plot_id_x, plot_id_z with data from plot_id
         *  - populate world column with PlotMain.config.getString("plot_world") - which will be set from previous release;
         */  
        
        /**
         *  `plot`
         */
//        int target_len = 6;
//        ArrayList<String> ids = new ArrayList<String>();
//        stmt = connection.createStatement();
//        String table = "plot";
//        ResultSet rs = stmt.executeQuery("SELECT * FROM `"+table+"`");
//        ResultSetMetaData md = rs.getMetaData();
//        int len = md.getColumnCount();
//        if (len<target_len) {
//            HashSet<String> cols = new HashSet<String>();
//            for (int i = 1; i <= len; i++) {
//                cols.add(md.getColumnName(i));
//            }
//            while (rs.next()) {
//                ids.add(rs.getString("plot_id"));
//            }
//        }
//        stmt.close();
    }

    /**
     * Delete a plot
     * @param plot
     */
    public static void delete(final String world, final Plot plot) {
        boolean result = PlotMain.removePlot(world,plot.id);
        if (result) {
            runTask(new Runnable() {
                @Override
                public void run() {
                    PreparedStatement stmt = null;
                    int id = getId(world,plot.id);
                    try {
                        stmt = connection.prepareStatement("DELETE FROM `plot_settings` WHERE `plot_plot_id` = ?");
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                        stmt.close();
                        stmt = connection.prepareStatement("DELETE FROM `plot_helpers` WHERE `plot_plot_id` = ?");
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                        stmt.close();
                        stmt = connection.prepareStatement("DELETE FROM `plot` WHERE `id` = ?");
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                        stmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Logger.add(LogLevel.DANGER, "Failed to delete plot " + plot.id);
                    }
                }
            });
        }
    }

    /**
     * Create plot settings
     * @param id
     * @param plot
     */
    public static void createPlotSettings(final int id, final Plot plot) {
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("INSERT INTO `plot_settings`(`plot_plot_id`) VALUES(" +
                            "?)");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public static int getId(String world, PlotId id2) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT `id` FROM `plot` WHERE `plot_id_x` = ? AND `plot_id_z` = ? AND world = ? ORDER BY `timestamp` ASC");
            stmt.setInt(1, id2.x);
            stmt.setInt(2, id2.y);
            stmt.setString(3, world);
            ResultSet r = stmt.executeQuery();
            int id = Integer.MAX_VALUE;
            while (r.next()) {
                id = r.getInt("id");
            }
            stmt.close();
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Integer.MAX_VALUE;
    }


    /**
     * Get a plot id
     * @param plot_id
     * @return
     */
    /*public static int getId(String world, PlotId id2) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet r = stmt.executeQuery("SELECT `id` FROM `plot` WHERE `plot_id_x` = '" + id2.x + "' AND `plot_id_z` = '" + id2.y + "' AND `world` = '" + world + "' ORDER BY `timestamp` ASC");
            int id = Integer.MAX_VALUE;
            while(r.next()) {
                id = r.getInt("id");
            }
            stmt.close();
            return id;
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return Integer.MAX_VALUE;
    }*/

    /**
     *
     * @return
     */
	public static HashMap<String, HashMap<PlotId, Plot>> getPlots() {
	    HashMap<String, HashMap<PlotId, Plot>> plots = new HashMap<String, HashMap<PlotId, Plot>>();
	    HashMap<String, World> worldMap = new HashMap<String, World>();
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet r = stmt.executeQuery("SELECT `id`, `plot_id_x`, `plot_id_z`, `owner`, `world` FROM `plot`");
            PlotId plot_id;
            int id;
            Plot p;
            World w;
            while (r.next()) {
                plot_id = new PlotId(r.getInt("plot_id_x"),r.getInt("plot_id_z"));
                id = r.getInt("id");
                String worldname = r.getString("world");
                // Quicker to get cache the UUID to the World than to convert each time.
                HashMap<String, Object> settings = getSettings(id);
                UUID owner = UUID.fromString(r.getString("owner"));
                Biome plotBiome = Biome.valueOf((String) settings.get("biome"));
                if(plotBiome == null) plotBiome = Biome.FOREST;
                String[] flags_string;
                if (settings.get("flags") == null)
                    flags_string = new String[] {};
                else
                    flags_string = ((String) settings.get("flags")).split(",");
                Flag[] flags = new Flag[flags_string.length];
                for (int i = 0; i<flags.length; i++) {
                    String[] split = flags_string[i].split(":");
                    flags[i] = new Flag(split[0], split[1]);
                }
                
                ArrayList<UUID> helpers = plotHelpers(id);
                ArrayList<UUID> denied = plotDenied(id);
                //boolean changeTime = ((Short) settings.get("custom_time") == 0) ? false : true;
                long time = 8000l;
                //if(changeTime) {
                //    time = Long.parseLong(settings.get("time").toString());
                //}
                //boolean rain = Integer.parseInt(settings.get("rain").toString()) == 1 ? true : false;
                boolean rain = false;
                String alias = (String) settings.get("alias");
                if(alias == null || alias.equalsIgnoreCase("NEW")) alias = "";
                PlotHomePosition position = null;
                for(PlotHomePosition plotHomePosition : PlotHomePosition.values())
                    if(plotHomePosition.isMatching((String)settings.get("position"))) position = plotHomePosition;
                if(position == null) position = PlotHomePosition.DEFAULT;
                
                p = new Plot(plot_id, owner, plotBiome, helpers, denied, /*changeTime*/ false, time, rain, alias, position, flags, worldname);
                if (plots.containsKey(worldname)) {
                    plots.get(worldname).put((plot_id), p);
                }
                else {
                    HashMap<PlotId, Plot> map = new HashMap<PlotId, Plot>();
                    map.put((plot_id), p);
                    plots.put(worldname, map);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            Logger.add(LogLevel.WARNING, "Failed to load plots.");
            e.printStackTrace();
        }
        return plots;
    }

    /**
     *
     * @param plot
     * @param rain
     */
	public static void setWeather(final String world, final Plot plot, final boolean rain) {
        plot.settings.setRain(rain);
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    int weather = rain ? 1 : 0;
                    PreparedStatement stmt = connection.prepareStatement("UPDATE `plot_settings` SET `rain` = ? WHERE `plot_plot_id` = ?");
                    stmt.setInt(1, weather);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.execute();
                    stmt.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Could not set weather for plot " + plot.id);
                }
            }
        });
    }
	public static void setFlags(final String world, final Plot plot, final Flag[] flags) {
        plot.settings.setFlags(flags);
        final StringBuilder flag_string = new StringBuilder();
        int i = 0;
        for (Flag flag:flags) {
            if (i!=0)
                flag_string.append(",");
            flag_string.append(flag.getKey()+":"+flag.getValue());
            i++;
        }
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement stmt = connection.prepareStatement("UPDATE `plot_settings` SET `flags` = ? WHERE `plot_plot_id` = ?");
                    stmt.setString(1, flag_string.toString());
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.execute();
                    stmt.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Could not set flag for plot " + plot.id);
                }
            }
        });
    }

    /**
     *
     * @param plot
     * @param alias
     */
    public static void setAlias(final String world, final Plot plot, final String alias) {
        plot.settings.setAlias(alias);
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("UPDATE `plot_settings` SET `alias` = ?  WHERE `plot_plot_id` = ?");
                    stmt.setString(1, alias);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.executeUpdate();
                    stmt.close();
                } catch(SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set alias for plot " + plot.id);
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     *
     * @param r
     */
    private static void runTask(Runnable r) {
        PlotMain.getMain().getServer().getScheduler().runTaskAsynchronously(PlotMain.getMain(), r);
    }

    /**
     *
     * @param plot
     * @param position
     */
    public static void setPosition(final String world,final Plot plot, final String position) {
        plot.settings.setPosition(PlotHomePosition.valueOf(position));
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("UPDATE `plot_settings` SET `position` = ?  WHERE `plot_plot_id` = ?");
                    stmt.setString(1, position);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.executeUpdate();
                    stmt.close();
                } catch(SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set position for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     *
     * @param id
     * @return
     */
    public static HashMap<String, Object> getSettings(int id) {
        HashMap<String, Object> h = new HashMap<String, Object>();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT * FROM `plot_settings` WHERE `plot_plot_id` = ?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            String var;
            Object val;
            while(r.next()) {
                var = "biome";
                val = r.getObject(var);
                h.put(var,val);
                var = "rain";
                val = r.getObject(var);
                h.put(var,val);
                var = "custom_time";
                val = r.getObject(var);
                h.put(var, val);
                var = "time";
                val = r.getObject(var);
                h.put(var,val);
                var = "deny_entry";
                val = r.getObject(var);
                h.put(var, (short) 0);
                var = "alias";
                val = r.getObject(var);
                h.put(var, val);
                var = "position";
                val = r.getObject(var);
                h.put(var, val);
                var = "flags";
                val = r.getObject(var);
                h.put(var, val);
            }
            stmt.close();;
        } catch(SQLException e) {
          Logger.add(LogLevel.WARNING, "Failed to load settings for plot: " + id);
            e.printStackTrace();
        }
        return h;
    }

    /**
     *
     */
    public static UUID everyone = UUID.fromString("1-1-3-3-7");

    /**
     *
     * @param id
     * @return
     */
    private static ArrayList<UUID> plotDenied(int id) {
        ArrayList<UUID> l = new ArrayList<UUID>();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT `user_uuid` FROM `plot_denied` WHERE `plot_plot_id` = ?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            UUID u;
            while(r.next()) {
                u = UUID.fromString(r.getString("user_uuid"));
                l.add(u);
            }
            stmt.close();
        } catch(Exception e) {
            Logger.add(LogLevel.DANGER, "Failed to load denied for plot: " + id);
            e.printStackTrace();
        }
        return l;
    }

    /**
     *
      * @param id
     * @return
     */
	private static ArrayList<UUID> plotHelpers(int id) {
		ArrayList<UUID> l = new ArrayList<UUID>();
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			ResultSet r = stmt.executeQuery("SELECT `user_uuid` FROM `plot_helpers` WHERE `plot_plot_id` = " + id);
			UUID u;
			while(r.next()) {
				u = UUID.fromString(r.getString("user_uuid"));
				l.add(u);
			}
			stmt.close();
		} catch (SQLException e) {
			Logger.add(LogLevel.WARNING, "Failed to load helpers for plot: " + id);
			e.printStackTrace();
		}
		return l;
	}

    /**
     *
     * @param plot
     * @param player
     */
    public static void removeHelper(final String world,final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM `plot_helpers` WHERE `plot_plot_id` = ? AND `user_uuid` = ?");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Failed to remove helper for plot " + plot.id);
                }
            }
        });
    }

    /**
     *
     * @param plot
     * @param player
     */
    public static void setHelper(final String world,final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO `plot_helpers` (`plot_plot_id`, `user_uuid`) VALUES(?,?)");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch(SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set helper for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     *
     * @param plot
     * @param player
     */
    public static void removeDenied(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM `plot_denied` WHERE `plot_plot_id` = ? AND `user_uuid` = ?");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Failed to remove denied for plot " + plot.id);
                }
            }
        });
    }

    /**
     *
     * @param plot
     * @param player
     */
    public static void setDenied(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO `plot_denied` (`plot_plot_id`, `user_uuid`) VALUES(?,?)");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch(SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set denied for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }
}