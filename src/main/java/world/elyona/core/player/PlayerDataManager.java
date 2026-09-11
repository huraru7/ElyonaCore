package world.elyona.core.player;

import org.bukkit.entity.Player;
import world.elyona.core.ElyonaCorePlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final ElyonaCorePlugin plugin;
    private final PlayerRepository repository;

    /** オンラインキャッシュ: UUID → PlayerRecord */
    private final Map<UUID, PlayerRepository.PlayerRecord> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(ElyonaCorePlugin plugin, PlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /**
     * ログイン時に呼び出す。upsert してキャッシュに格納。
     * @return initial_grant_done の状態を含む PlayerRecord
     */
    public CompletableFuture<PlayerRepository.PlayerRecord> onJoin(Player player) {
        return repository.upsertPlayer(player.getUniqueId(), player.getName())
                .thenApply(record -> {
                    if (record != null) {
                        cache.put(player.getUniqueId(), record);
                    }
                    return record;
                });
    }

    /** ログアウト時にキャッシュを削除し、last_seen を更新 */
    public void onQuit(UUID uuid) {
        cache.remove(uuid);
        repository.updateLastSeen(uuid);
    }

    /** 初回付与フラグを立てる（DBとキャッシュ両方更新） */
    public void markInitialGrantDone(UUID uuid) {
        repository.markInitialGrantDone(uuid);
        PlayerRepository.PlayerRecord old = cache.get(uuid);
        if (old != null) {
            cache.put(uuid, new PlayerRepository.PlayerRecord(old.uuid(), old.name(), true));
        }
    }

    /** キャッシュから PlayerRecord 取得 */
    public PlayerRepository.PlayerRecord getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean isInitialGrantDone(UUID uuid) {
        PlayerRepository.PlayerRecord r = cache.get(uuid);
        return r != null && r.initialGrantDone();
    }

    public void saveLastSeen(UUID uuid) {
        repository.updateLastSeen(uuid);
    }
}
