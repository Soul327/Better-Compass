package me.soul327.compass;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Compass extends JavaPlugin implements Listener {
    static boolean highlightPlayers = false;
    static int duration = 10;
    int compassUpdateInterval = 5;

    ArrayList<Player> players = new ArrayList<>();
    Map<Player, Player> data = new HashMap<>();

    @Override
    public void onEnable() {
        // Load config
        loadConfig();

        // Load classes
        PlayerHighlight playerHighlight = new PlayerHighlight(this);

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(playerHighlight, this);

        // Schedule events
        Bukkit.getScheduler().runTaskTimer(this, this::updateCompasses, 0, compassUpdateInterval);

        players.addAll( Bukkit.getOnlinePlayers() );
    }

    public void loadConfig() {
        // Load config
        FileConfiguration config = this.getConfig();

        // Set default values if the config does not exist
        config.addDefault("highlight-players", false);
        config.addDefault("highlight-duration", 20 * 60);
        config.addDefault("compass-update-interval", 5);

        config.options().copyDefaults(true);
        saveConfig();

        // Load the values from the config
        highlightPlayers = config.getBoolean("highlight-players");
        duration = config.getInt("highlight-duration");
        compassUpdateInterval = config.getInt("compass-update-interval");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        players.add( player );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Called when a player leaves a server
        Player player = event.getPlayer();
        players.remove( player );
    }

    @EventHandler
    public void onPlayerClicks(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        // Check if compass is in the player's main hand
        if(player.getInventory().getItemInMainHand().getType() != Material.COMPASS)
            return;

        // Check if the item is a compass
        if ( item == null || item.getType() != Material.COMPASS )
            return;

        // Load data
        Player targetPlayer = data.get(player);
        int num = players.indexOf(targetPlayer);

        if ( action.equals( Action.RIGHT_CLICK_AIR ) || action.equals( Action.RIGHT_CLICK_BLOCK ) ) {
            // Forward
            num++;
        }

        if ( action.equals( Action.LEFT_CLICK_AIR ) || action.equals( Action.LEFT_CLICK_BLOCK ) ) {
            // Backwards
            num--;
        }

        if(num <= 0) num = players.size() - 1;
        if(num > players.size() - 1) num = 0;

        // Update data
        targetPlayer = players.get(num);
        data.put(player, targetPlayer);

        updateCompass(player);
        if(player != targetPlayer) {
            player.sendMessage("Compass is now pointing to " + targetPlayer.getDisplayName());
            return;
        }

        player.sendMessage("Compass is now pointing to where you will respawn");
    }

    public void updateCompasses() {
        // Update compass
        data.keySet().forEach(this::updateCompass);
    }

    public void updateCompass(Player player) {
        Player targetPlayer = data.get(player);
        if(targetPlayer != null && targetPlayer != player) {
            player.setCompassTarget( targetPlayer.getLocation() );
            return;
        }

        if( player.getBedSpawnLocation() != null ) {
            player.setCompassTarget( player.getBedSpawnLocation() );
            return;
        }

        player.setCompassTarget(Bukkit.getServer().getWorld("world").getSpawnLocation());
    }
}
