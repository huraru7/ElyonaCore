package world.elyona.core.rank;

import world.elyona.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RankRepository {

    private final DatabaseManager db;

    public RankRepository(DatabaseManager db) {
        this.db = db;
    }

    // ---- Seasons ----

    /** 新シーズンをINSERTしてseason_idを返す */
    public CompletableFuture<Integer> createSeason(String name) {
        String sql = "INSERT INTO elyona_seasons (name, started_at) VALUES (?, CURRENT_TIMESTAMP)";
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return -1;
        });
    }

    /** 現在進行中のシーズン（ended_at IS NULL）を取得 */
    public CompletableFuture<SeasonRecord> getActiveSeason() {
        return db.queryAsync(
                "SELECT season_id, name, started_at FROM elyona_seasons WHERE ended_at IS NULL ORDER BY season_id DESC LIMIT 1",
                ps -> {},
                rs -> rs.next() ? new SeasonRecord(rs.getInt("season_id"), rs.getString("name"), rs.getTimestamp("started_at")) : null
        );
    }

    /** シーズンを終了させる */
    public CompletableFuture<Void> endSeason(int seasonId) {
        return db.executeAsync(
                "UPDATE elyona_seasons SET ended_at = CURRENT_TIMESTAMP WHERE season_id = ?",
                ps -> ps.setInt(1, seasonId)
        );
    }

    // ---- Ranks ----

    /** プレイヤーのランクレコードを取得 */
    public CompletableFuture<RankRecord> getRank(UUID uuid, int seasonId) {
        return db.queryAsync(
                "SELECT rank_tier, exp_total FROM elyona_ranks WHERE uuid = ? AND season_id = ?",
                ps -> {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, seasonId);
                },
                rs -> rs.next()
                        ? new RankRecord(uuid, seasonId, RankTier.valueOf(rs.getString("rank_tier")), rs.getLong("exp_total"))
                        : new RankRecord(uuid, seasonId, RankTier.BRONZE, 0L)
        );
    }

    /** ランクレコードをUPSERT */
    public CompletableFuture<Void> saveRank(UUID uuid, int seasonId, RankTier tier, long exp) {
        String sql = db.isMySql()
                ? "INSERT INTO elyona_ranks (uuid, season_id, rank_tier, exp_total) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE rank_tier=?, exp_total=?"
                : "INSERT INTO elyona_ranks (uuid, season_id, rank_tier, exp_total) VALUES (?, ?, ?, ?) ON CONFLICT(uuid, season_id) DO UPDATE SET rank_tier=excluded.rank_tier, exp_total=excluded.exp_total";

        return db.executeAsync(sql, ps -> {
            ps.setString(1, uuid.toString());
            ps.setInt(2, seasonId);
            ps.setString(3, tier.name());
            ps.setLong(4, exp);
            if (db.isMySql()) {
                ps.setString(5, tier.name());
                ps.setLong(6, exp);
            }
        });
    }

    /** シーズン終了時に全プレイヤーをBronzeにリセット（バッチUPDATE） */
    public CompletableFuture<Void> resetAllRanks(int seasonId) {
        return db.executeAsync(
                "UPDATE elyona_ranks SET rank_tier = 'BRONZE', exp_total = 0 WHERE season_id = ?",
                ps -> ps.setInt(1, seasonId)
        );
    }

    public record SeasonRecord(int seasonId, String name, Timestamp startedAt) {}
    public record RankRecord(UUID uuid, int seasonId, RankTier tier, long expTotal) {}
}
