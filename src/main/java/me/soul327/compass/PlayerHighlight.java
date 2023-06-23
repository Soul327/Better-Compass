package me.soul327.compass;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static me.soul327.compass.Compass.duration;
import static me.soul327.compass.Compass.highlightPlayers;

public class PlayerHighlight implements Listener {
    Compass plugin;

    public PlayerHighlight(Compass plugin) {
        this.plugin = plugin;

        highlightPlayers();
        // Bukkit.getScheduler().runTaskTimer(this, this::highlightPlayers, 0, duration);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applySpectralHighlight(player);
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

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PotionEffectType effectType = event.getOldEffect().getType();

        if(
            event.getAction() != EntityPotionEffectEvent.Action.REMOVED ||
            event.getAction() != EntityPotionEffectEvent.Action.CLEARED
        ) return;

        // We check it this way because checking them directly always returns false
        if(!effectType.getName().equals(PotionEffectType.GLOWING.getName())) return;

        // Apply glowing
        applySpectralHighlight( player );
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
}
