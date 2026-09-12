package world.elyona.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import world.elyona.core.player.PlayerDataManager;

public class PlayerQuitListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public PlayerQuitListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // プレイヤーデータ更新・キャッシュ削除
        // ランク・称号データの保存はElyonaRank側が独立してPlayerQuitEventを購読する（ドメイン分割）
        playerDataManager.onQuit(player.getUniqueId());
    }
}
