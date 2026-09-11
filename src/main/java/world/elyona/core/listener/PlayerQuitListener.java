package world.elyona.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import world.elyona.core.player.PlayerDataManager;
import world.elyona.core.rank.SeasonManager;
import world.elyona.core.title.TitleManager;

public class PlayerQuitListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public PlayerQuitListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // プレイヤーデータ更新・キャッシュ削除
        playerDataManager.onQuit(player.getUniqueId());

        // ランクをDBに書き込む（SeasonManagerはElyonaCorePluginから取得）
        world.elyona.core.ElyonaCorePlugin plugin = world.elyona.core.ElyonaCorePlugin.getInstance();
        plugin.getSeasonManager().saveAndRemoveRank(player.getUniqueId());

        // 称号キャッシュ削除
        plugin.getTitleManager().unloadActiveTitle(player.getUniqueId());
    }
}
