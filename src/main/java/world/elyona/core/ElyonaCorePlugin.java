package world.elyona.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.command.RankCommand;
import world.elyona.core.command.SeasonCommand;
import world.elyona.core.command.TitleCommand;
import world.elyona.core.config.PluginConfig;
import world.elyona.core.database.DatabaseManager;
import world.elyona.core.listener.GuiListener;
import world.elyona.core.listener.PlayerJoinListener;
import world.elyona.core.listener.PlayerQuitListener;
import world.elyona.core.listener.RankUpListener;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.core.player.PlayerDataManager;
import world.elyona.core.player.PlayerRepository;
import world.elyona.core.rank.RankManager;
import world.elyona.core.rank.RankRepository;
import world.elyona.core.rank.SeasonManager;
import world.elyona.core.title.TitleLoader;
import world.elyona.core.title.TitleManager;
import world.elyona.core.title.TitleRepository;

public class ElyonaCorePlugin extends JavaPlugin {

    private static ElyonaCorePlugin instance;

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private MimicMessenger mimicMessenger;
    private PlayerRepository playerRepository;
    private PlayerDataManager playerDataManager;
    private SeasonManager seasonManager;
    private RankRepository rankRepository;
    private RankManager rankManager;
    private TitleLoader titleLoader;
    private TitleRepository titleRepository;
    private TitleManager titleManager;

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

        // ランクシステム
        rankRepository = new RankRepository(databaseManager);
        seasonManager = new SeasonManager(this, rankRepository);
        rankManager = new RankManager(this, rankRepository, seasonManager);

        // 称号システム
        titleLoader = new TitleLoader(this);
        titleRepository = new TitleRepository(databaseManager);
        titleManager = new TitleManager(this, titleRepository, titleLoader, mimicMessenger);

        // リスナー登録
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this, playerDataManager, rankRepository, seasonManager, titleRepository), this);
        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(playerDataManager), this);
        getServer().getPluginManager().registerEvents(
                new RankUpListener(this, rankManager, titleManager, mimicMessenger), this);
        getServer().getPluginManager().registerEvents(
                new GuiListener(), this);

        // コマンド登録
        getCommand("rank").setExecutor(new RankCommand(rankManager, seasonManager));
        getCommand("season").setExecutor(new SeasonCommand(this, seasonManager, rankManager, mimicMessenger));
        getCommand("title").setExecutor(new TitleCommand(titleManager, mimicMessenger));

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
    public SeasonManager getSeasonManager() { return seasonManager; }
    public RankManager getRankManager() { return rankManager; }
    public TitleManager getTitleManager() { return titleManager; }
    public TitleLoader getTitleLoader() { return titleLoader; }
}
