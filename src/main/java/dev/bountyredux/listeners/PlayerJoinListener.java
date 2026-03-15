package dev.bountyredux.listeners;

import dev.bountyredux.BountiesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.profile.PlayerTextures;

public class PlayerJoinListener implements Listener {

    private final BountiesPlugin plugin;

    public PlayerJoinListener(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Grab the skin property straight from their profile
        // Works for both premium and SkinRestorer cracked players
        player.getPlayerProfile().getProperties().stream()
                .filter(prop -> prop.getName().equals("textures"))
                .findFirst()
                .ifPresent(prop -> {
                    plugin.getDatabaseManager().cacheTexture(
                            player.getName(),
                            prop.getValue(),
                            prop.getSignature() != null ? prop.getSignature() : ""
                    );
                });
    }
}