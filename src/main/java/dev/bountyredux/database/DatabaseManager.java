package dev.bountyredux.database;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.model.Bounty;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles all SQLite persistence for bounties.
 *
 * Schema:
 *   bounties(id, target_uuid, target_name, placer_uuid, placer_name, amount, created_at)
 *   uuid_cache(player_name, player_uuid)
 *   texture_cache(player_name, skin_value, skin_signature)
 */
public class DatabaseManager {

    private final BountiesPlugin plugin;
    private Connection connection;

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS bounties (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                target_uuid TEXT    NOT NULL,
                target_name TEXT    NOT NULL,
                placer_uuid TEXT    NOT NULL,
                placer_name TEXT    NOT NULL,
                amount      REAL    NOT NULL,
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            );
            """;

    private static final String CREATE_UUID_CACHE = """
            CREATE TABLE IF NOT EXISTS uuid_cache (
                player_name TEXT PRIMARY KEY COLLATE NOCASE,
                player_uuid TEXT NOT NULL
            );
            """;

    private static final String CREATE_TEXTURE_CACHE = """
            CREATE TABLE IF NOT EXISTS texture_cache (
                player_name     TEXT PRIMARY KEY COLLATE NOCASE,
                skin_value      TEXT NOT NULL,
                skin_signature  TEXT NOT NULL
            );
            """;

    private static final String INSERT_BOUNTY =
            "INSERT INTO bounties (target_uuid, target_name, placer_uuid, placer_name, amount) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_TARGET =
            "SELECT * FROM bounties WHERE target_uuid = ? ORDER BY created_at ASC";

    private static final String DELETE_BY_TARGET =
            "DELETE FROM bounties WHERE target_uuid = ?";

    private static final String SELECT_TOP = """
            SELECT target_uuid, target_name, SUM(amount) AS total
            FROM bounties
            GROUP BY target_uuid
            ORDER BY total DESC
            LIMIT ?
            """;

    private static final String UPSERT_UUID =
            "INSERT OR REPLACE INTO uuid_cache (player_name, player_uuid) VALUES (?, ?)";

    private static final String SELECT_UUID =
            "SELECT player_uuid FROM uuid_cache WHERE player_name = ? COLLATE NOCASE";

    private static final String UPSERT_TEXTURE =
            "INSERT OR REPLACE INTO texture_cache (player_name, skin_value, skin_signature) VALUES (?, ?, ?)";

    private static final String SELECT_TEXTURE =
            "SELECT skin_value, skin_signature FROM texture_cache WHERE player_name = ? COLLATE NOCASE";

    public DatabaseManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            File dbFile = new File(plugin.getDataFolder(), "bounties.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
            }

            initTable();
            plugin.getLogger().info("SQLite database connected.");

        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found! Check pom.xml shading.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite!", e);
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQLite connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing SQLite connection.", e);
        }
    }

    private void initTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE);
            stmt.execute(CREATE_UUID_CACHE);
            stmt.execute(CREATE_TEXTURE_CACHE);
        }
    }

    // ── Bounty CRUD ───────────────────────────────────────────────────────────

    public void insertBounty(Bounty bounty) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_BOUNTY)) {
            ps.setString(1, bounty.getTargetUUID().toString());
            ps.setString(2, bounty.getTargetName());
            ps.setString(3, bounty.getPlacedByUUID().toString());
            ps.setString(4, bounty.getPlacedByName());
            ps.setDouble(5, bounty.getAmount());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert bounty: " + bounty, e);
        }
    }

    public List<Bounty> loadAllBounties() {
        List<Bounty> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bounties ORDER BY created_at ASC")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load bounties from DB!", e);
        }
        return list;
    }

    public int deleteBounties(UUID targetUUID) {
        try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_TARGET)) {
            ps.setString(1, targetUUID.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete bounties for " + targetUUID, e);
            return 0;
        }
    }

    // ── UUID cache ────────────────────────────────────────────────────────────

    public void cacheUUID(String playerName, UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_UUID)) {
            ps.setString(1, playerName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to cache UUID for " + playerName, e);
        }
    }

    public UUID getCachedUUID(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_UUID)) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UUID.fromString(rs.getString("player_uuid"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get cached UUID for " + playerName, e);
        }
        return null;
    }

    // ── Texture cache ─────────────────────────────────────────────────────────

    public void cacheTexture(String playerName, String value, String signature) {
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_TEXTURE)) {
            ps.setString(1, playerName);
            ps.setString(2, value);
            ps.setString(3, signature);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to cache texture for " + playerName, e);
        }
    }

    public String[] getCachedTexture(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_TEXTURE)) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{
                    rs.getString("skin_value"),
                    rs.getString("skin_signature")
            };
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get cached texture for " + playerName, e);
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bounty mapRow(ResultSet rs) throws SQLException {
        return new Bounty(
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                UUID.fromString(rs.getString("placer_uuid")),
                rs.getString("placer_name"),
                rs.getDouble("amount")
        );
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}