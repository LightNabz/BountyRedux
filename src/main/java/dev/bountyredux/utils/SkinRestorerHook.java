package dev.bountyredux.utils;

import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Optional;
import java.util.UUID;

public class SkinRestorerHook {

    private static SkinsRestorer api = null;
    private static boolean available = false;

    public static void setup() {
        if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") == null) return;
        try {
            api = SkinsRestorerProvider.get();
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Tries to apply SkinRestorer's stored skin onto a SkullMeta.
     * Returns true if skin was applied, false if SR has nothing for this player.
     */
    public static boolean applyStoredSkin(SkullMeta meta, UUID playerUUID, String playerName) {
        if (!available || api == null) return false;
        try {
            PlayerStorage playerStorage = api.getPlayerStorage();
            Optional<SkinProperty> skinProp = playerStorage.getSkinForPlayer(playerUUID, playerName);
            if (skinProp.isEmpty()) return false;

            SkinProperty prop = skinProp.get();

            // Build a PlayerProfile and inject the SR texture into it
            PlayerProfile profile = Bukkit.createPlayerProfile(playerUUID, playerName);
            PlayerTextures textures = profile.getTextures();

            // SR gives us the base64 value — decode the skin URL from it
            String decoded = new String(java.util.Base64.getDecoder().decode(prop.getValue()));
            // Extract URL from the decoded JSON: {"textures":{"SKIN":{"url":"..."}}}
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd   = decoded.indexOf("\"", urlStart);
            if (urlStart < 7 || urlEnd < 0) return false;

            textures.setSkin(new URL(decoded.substring(urlStart, urlEnd)));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}