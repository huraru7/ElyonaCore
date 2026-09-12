package world.elyona.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.player.PlayerDataManager;

public class PlayerJoinListener implements Listener {

    private final ElyonaCorePlugin plugin;
    private final PlayerDataManager playerDataManager;

    public PlayerJoinListener(ElyonaCorePlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // プレイヤーデータのupsertとキャッシュ
        // ランク・称号データのロードはElyonaRank側が独立してPlayerJoinEventを購読する（ドメイン分割）
        playerDataManager.onJoin(player).exceptionally(e -> {
            plugin.getLogger().warning("プレイヤーデータ読み込みエラー(" + player.getName() + "): " + e.getMessage());
            return null;
        });
    }
}
