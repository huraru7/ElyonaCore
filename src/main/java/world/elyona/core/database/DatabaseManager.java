package world.elyona.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.config.PluginConfig;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {

    private final ElyonaCorePlugin plugin;
    private final PluginConfig config;
    private HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public DatabaseManager(ElyonaCorePlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        HikariConfig hikari = new HikariConfig();

        if (config.isMySql()) {
            hikari.setJdbcUrl("jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort()
                    + "/" + config.getMysqlDatabase() + "?useSSL=false&characterEncoding=UTF-8");
            hikari.setUsername(config.getMysqlUsername());
            hikari.setPassword(config.getMysqlPassword());
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), config.getSqliteFile());
            plugin.getDataFolder().mkdirs();
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(1); // SQLite は同時接続1
        }

        hikari.setPoolName("ElyonaCore-DB");
        hikari.setMaximumPoolSize(config.isMySql() ? 10 : 1);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(hikari);
        plugin.getLogger().info("データベース接続を確立しました。(" + (config.isMySql() ? "MySQL" : "SQLite") + ")");

        createTables();
        insertServerAccount();
    }

    private void createTables() {
        boolean mysql = config.isMySql();
        String autoInc = mysql ? "INT AUTO_INCREMENT" : "INTEGER";

        String[] sqls = {
            "CREATE TABLE IF NOT EXISTS elyona_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",
            "CREATE TABLE IF NOT EXISTS elyona_titles (" +
                "uuid VARCHAR(36) NOT NULL," +
                "title_id VARCHAR(64) NOT NULL," +
                "obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (uuid, title_id)," +
                "FOREIGN KEY (uuid) REFERENCES elyona_players(uuid)" +
            ")",
            "CREATE TABLE IF NOT EXISTS elyona_active_title (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "title_id VARCHAR(64)," +
                "FOREIGN KEY (uuid) REFERENCES elyona_players(uuid)" +
            ")",
            "CREATE TABLE IF NOT EXISTS elyona_seasons (" +
                "season_id " + autoInc + " PRIMARY KEY," +
                "name VARCHAR(64) NOT NULL," +
                "started_at TIMESTAMP NOT NULL," +
                "ended_at TIMESTAMP" +
            ")",
            "CREATE TABLE IF NOT EXISTS elyona_ranks (" +
                "uuid VARCHAR(36) NOT NULL," +
                "season_id INT NOT NULL," +
                "rank_tier VARCHAR(16) DEFAULT 'BRONZE'," +
                "exp_total BIGINT DEFAULT 0," +
                "PRIMARY KEY (uuid, season_id)," +
                "FOREIGN KEY (uuid) REFERENCES elyona_players(uuid)," +
                "FOREIGN KEY (season_id) REFERENCES elyona_seasons(season_id)" +
            ")"
        };

        try (Connection conn = dataSource.getConnection()) {
            for (String sql : sqls) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("テーブル作成に失敗しました: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void insertServerAccount() {
        String serverUuid = "00000000-0000-0000-0000-000000000000";
        String insertPlayer = config.isMySql()
                ? "INSERT IGNORE INTO elyona_players (uuid, name) VALUES (?, 'SERVER')"
                : "INSERT OR IGNORE INTO elyona_players (uuid, name) VALUES (?, 'SERVER')";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertPlayer)) {
            ps.setString(1, serverUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("サーバー口座（プレイヤーレコード）の挿入に失敗しました: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** 非同期クエリ実行（ResultSet を処理するラムダを受け取る） */
    public <T> CompletableFuture<T> queryAsync(String sql, SqlConsumer<PreparedStatement> prepare, SqlFunction<ResultSet, T> handler) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                prepare.accept(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return handler.apply(rs);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /** 非同期更新実行（INSERT/UPDATE/DELETE） */
    public CompletableFuture<Void> executeAsync(String sql, SqlConsumer<PreparedStatement> prepare) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                prepare.accept(ps);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /** 同期更新（onDisable 等、メインスレッドシャットダウン時に使用） */
    public void executeSync(String sql, SqlConsumer<PreparedStatement> prepare) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            prepare.accept(ps);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("DB同期実行エラー: " + e.getMessage());
        }
    }

    public boolean isMySql() {
        return config.isMySql();
    }

    public void close() {
        executor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("データベース接続を閉じました。");
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T t) throws Exception;
    }
}
