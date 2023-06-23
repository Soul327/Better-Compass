package me.soul327.compass;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class Compass extends JavaPlugin implements Listener {
    boolean highlightPlayers = false;
    int duration = 10;
    int compassUpdateInterval = 5;

    ArrayList<Player> players = new ArrayList<>();
    Map<Player, Player> data = new HashMap<>();

    @Override
    public void onEnable() {
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

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::updateCompasses, 0, compassUpdateInterval);
        if(highlightPlayers) {
            // Bukkit.getScheduler().runTaskTimer(this, this::highlightPlayers, 0, duration);
            highlightPlayers();
        }

        players.addAll( Bukkit.getOnlinePlayers() );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(highlightPlayers) applySpectralHighlight(player);
        players.add( player );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Called when a player leaves a server
        Player player = event.getPlayer();
        String quitMessage = event.getQuitMessage();

        Bukkit.broadcastMessage(quitMessage);
        players.remove( player );
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Check if the player crouched
        if (event.isSneaking()) {
            // Clear glowing effect set by the plugin
            player.removePotionEffect(PotionEffectType.GLOWING);
            return;
        }
        // Attempt to highlight the player, but skip the crouching check as the player is not counted as not sneaking
        // until after this function is called
        applySpectralHighlight(player, true);

    }


    public void highlightPlayers() {
        Bukkit.getOnlinePlayers().forEach( this::applySpectralHighlight );
    }

    public void applySpectralHighlight(Player player) {
        applySpectralHighlight(player, false);
    }
    public void applySpectralHighlight(Player player, Boolean skipSneakingCheck) {
        // Skip players that are sneaking
        if( !skipSneakingCheck && player.isSneaking() ) return;

        // Skip players that are invisible
        if( player.hasPotionEffect(PotionEffectType.INVISIBILITY) ) return;

        // Check config
        if(!highlightPlayers) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 2, 0, false, false));
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

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PotionEffectType effectType = event.getOldEffect().getType();

        if(event.getAction() != EntityPotionEffectEvent.Action.REMOVED) return;

        // We check it this way because checking them directly always returns false
        if(!effectType.getName().equals(PotionEffectType.GLOWING.getName())) return;

        // Apply glowing
        applySpectralHighlight( player );
    }
}
