package world.elyona.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.config.PluginConfig;
import world.elyona.core.database.DatabaseManager;
import world.elyona.core.listener.PlayerJoinListener;
import world.elyona.core.listener.PlayerQuitListener;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.core.player.PlayerDataManager;
import world.elyona.core.player.PlayerRepository;

public class ElyonaCorePlugin extends JavaPlugin {

    private static ElyonaCorePlugin instance;

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private MimicMessenger mimicMessenger;
    private PlayerRepository playerRepository;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        // 設定読み込み
        pluginConfig = new PluginConfig(this);

        // DB初期化
        databaseManager = new DatabaseManager(this, pluginConfig);
        databaseManager.initialize();

        // MIMICメッセージ
        mimicMessenger = new MimicMessenger(pluginConfig);

        // プレイヤーデータ
        playerRepository = new PlayerRepository(databaseManager);
        playerDataManager = new PlayerDataManager(this, playerRepository);

        // リスナー登録
        // ランク・称号システムはElyonaRankプラグインへ分割済み
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this, playerDataManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(playerDataManager), this);

        getLogger().info("ElyonaCore が有効化されました。");
    }

    @Override
    public void onDisable() {
        // オンラインプレイヤーのデータを全て保存
        if (playerDataManager != null) {
            Bukkit.getOnlinePlayers().forEach(p -> playerDataManager.saveLastSeen(p.getUniqueId()));
        }
        // DB接続を閉じる
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("ElyonaCore が無効化されました。");
    }

    public static ElyonaCorePlugin getInstance() { return instance; }
    public PluginConfig getPluginConfig() { return pluginConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MimicMessenger getMimicMessenger() { return mimicMessenger; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}
