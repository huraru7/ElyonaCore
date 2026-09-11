package world.elyona.core.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.economy.EconomyCache;
import world.elyona.core.player.PlayerDataManager;
import world.elyona.core.rank.RankRepository;
import world.elyona.core.rank.SeasonManager;
import world.elyona.core.title.TitleRepository;

public class PlayerJoinListener implements Listener {

    private final ElyonaCorePlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final EconomyCache economyCache;
    private final RankRepository rankRepository;
    private final SeasonManager seasonManager;
    private final TitleRepository titleRepository;

    public PlayerJoinListener(ElyonaCorePlugin plugin, PlayerDataManager playerDataManager,
                               EconomyCache economyCache, RankRepository rankRepository,
                               SeasonManager seasonManager, TitleRepository titleRepository) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.economyCache = economyCache;
        this.rankRepository = rankRepository;
        this.seasonManager = seasonManager;
        this.titleRepository = titleRepository;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. プレイヤーデータのupsertとキャッシュ
        playerDataManager.onJoin(player).thenCompose(record -> {
            // 2. 残高ロード
            return economyCache.load(player.getUniqueId()).thenApply(balance -> record);
        }).thenCompose(record -> {
            // 3. アクティブ称号ロード
            return plugin.getTitleManager().loadActiveTitle(player.getUniqueId()).thenApply(v -> record);
        }).thenAccept(record -> {
            if (record == null) return;

            // 4. 初回参加報酬（メインスレッドで処理）
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!record.initialGrantDone()) {
                    long grant = plugin.getPluginConfig().getInitialGrant();
                    economyCache.addBalance(player.getUniqueId(), grant);
                    playerDataManager.markInitialGrantDone(player.getUniqueId());
                    plugin.getMimicMessenger().sendTo(player,
                            "初回参加ボーナスとして " + grant + " Cr を受け取りました！");
                    plugin.getLogger().info("[初回ボーナス] " + player.getName() + " に " + grant + " Cr 付与");
                }

                // 5. ランクキャッシュロード
                seasonManager.loadRank(player.getUniqueId());
            });
        }).exceptionally(e -> {
            plugin.getLogger().warning("プレイヤーデータ読み込みエラー(" + player.getName() + "): " + e.getMessage());
            return null;
        });
    }
}
