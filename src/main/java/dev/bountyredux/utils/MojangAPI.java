package dev.bountyredux.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Fetches player UUID from Mojang API by name.
 * Used to resolve skulls for players who haven't joined the server.
 */
public class MojangAPI {

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";

    /**
     * Fetches the UUID for a given player name from Mojang.
     * Returns null if the player doesn't exist or the request fails.
     * MUST be called async — never on the main thread.
     */
    public static UUID fetchUUID(String playerName) {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(UUID_URL + playerName).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 204 || conn.getResponseCode() == 404) return null;

            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())).getAsJsonObject();

            String rawId = json.get("id").getAsString();
            // Mojang returns UUID without dashes — reformat it
            return UUID.fromString(rawId.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            ));

        } catch (Exception e) {
            return null;
        }
    }
}