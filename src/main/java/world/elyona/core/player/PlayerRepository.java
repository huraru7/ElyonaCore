package world.elyona.core.player;

import world.elyona.core.database.DatabaseManager;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {

    private final DatabaseManager db;

    public PlayerRepository(DatabaseManager db) {
        this.db = db;
    }

    /** プレイヤーが存在しなければ挿入、存在すれば name を更新 */
    public CompletableFuture<PlayerRecord> upsertPlayer(UUID uuid, String name) {
        String uuidStr = uuid.toString();
        String upsert = db.isMySql()
                ? "INSERT INTO elyona_players (uuid, name, last_seen) VALUES (?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE name=?, last_seen=CURRENT_TIMESTAMP"
                : "INSERT INTO elyona_players (uuid, name, last_seen) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, last_seen=CURRENT_TIMESTAMP";
        String upsertEco = db.isMySql()
                ? "INSERT IGNORE INTO elyona_economy (uuid, balance) VALUES (?, 0)"
                : "INSERT OR IGNORE INTO elyona_economy (uuid, balance) VALUES (?, 0)";

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.getConnection()) {
                // プレイヤー upsert
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, uuidStr);
                    ps.setString(2, name);
                    if (db.isMySql()) ps.setString(3, name);
                    ps.executeUpdate();
                }
                // economy レコードを確実に作成
                try (PreparedStatement ps = conn.prepareStatement(upsertEco)) {
                    ps.setString(1, uuidStr);
                    ps.executeUpdate();
                }
                // 最新レコードを返す
                return fetchPlayer(conn, uuidStr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<PlayerRecord> getPlayer(UUID uuid) {
        return db.queryAsync(
                "SELECT uuid, name, initial_grant_done FROM elyona_players WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString()),
                rs -> rs.next() ? new PlayerRecord(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getBoolean("initial_grant_done")
                ) : null
        );
    }

    public CompletableFuture<Void> markInitialGrantDone(UUID uuid) {
        return db.executeAsync(
                "UPDATE elyona_players SET initial_grant_done = 1 WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString())
        );
    }

    public CompletableFuture<Void> updateLastSeen(UUID uuid) {
        return db.executeAsync(
                "UPDATE elyona_players SET last_seen = CURRENT_TIMESTAMP WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString())
        );
    }

    private PlayerRecord fetchPlayer(Connection conn, String uuidStr) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid, name, initial_grant_done FROM elyona_players WHERE uuid = ?")) {
            ps.setString(1, uuidStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerRecord(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getBoolean("initial_grant_done")
                    );
                }
            }
        }
        return null;
    }

    public record PlayerRecord(UUID uuid, String name, boolean initialGrantDone) {}
}
