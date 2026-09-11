package world.elyona.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.economy.EconomyCache;
import world.elyona.core.event.ElyonaRankUpEvent;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.core.rank.RankManager;
import world.elyona.core.rank.RankTier;
import world.elyona.core.title.TitleManager;

public class RankUpListener implements Listener {

    private final ElyonaCorePlugin plugin;
    private final RankManager rankManager;
    private final TitleManager titleManager;
    private final EconomyCache economyCache;
    private final MimicMessenger mimic;

    public RankUpListener(ElyonaCorePlugin plugin, RankManager rankManager,
                          TitleManager titleManager, EconomyCache economyCache, MimicMessenger mimic) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.titleManager = titleManager;
        this.economyCache = economyCache;
        this.mimic = mimic;
    }

    @EventHandler
    public void onRankUp(ElyonaRankUpEvent event) {
        RankTier newRank = event.getNewRank();
        var player = event.getPlayer();
        String sym = plugin.getPluginConfig().getCurrencySymbol();

        // 本人への通知
        mimic.sendTo(player, "ランクアップ！ " + event.getOldRank().displayName + " → " + newRank.displayName);

        // 全体通知
        mimic.broadcast(player.getName() + " が " + newRank.displayName + " にランクアップしました！");

        // Cr報酬付与
        if (newRank.crReward > 0) {
            economyCache.addBalance(player.getUniqueId(), newRank.crReward);
            mimic.sendTo(player, "ランクアップ報酬として " + sym + " " + newRank.crReward + " を受け取りました！");
            plugin.getLogger().info("[RankUp] " + player.getName() + " Cr報酬 " + newRank.crReward);
        }

        // 称号付与
        if (newRank.titleId != null) {
            titleManager.grant(player, newRank.titleId);
        }
    }
}
