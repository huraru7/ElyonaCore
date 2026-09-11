package world.elyona.core.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.player.PlayerDataManager;

import java.util.List;
import java.util.UUID;

/**
 * Vault Economy 実装。
 * 残高操作はすべて EconomyCache 経由で行い、取引税や ElyonaPayEvent の発火は PayCommand 側が担当する。
 */
@SuppressWarnings("deprecation")
public class CredEconomy implements Economy {

    private final ElyonaCorePlugin plugin;
    private final EconomyCache cache;
    private final PlayerDataManager playerDataManager;

    public CredEconomy(ElyonaCorePlugin plugin, EconomyCache cache, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.cache = cache;
        this.playerDataManager = playerDataManager;
    }

    @Override public boolean isEnabled() { return plugin.isEnabled(); }
    @Override public String getName() { return "Cred"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 0; }
    @Override public String format(double amount) { return (long) amount + " Cr"; }
    @Override public String currencyNamePlural() { return "Cred"; }
    @Override public String currencyNameSingular() { return "Cred"; }

    // --- Account ---

    @Override
    public boolean hasAccount(String playerName) {
        // 名前ベースは非推奨だが互換のためキャッシュを全走査
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }

    @Override
    public boolean createPlayerAccount(String playerName) { return false; }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) { return false; }
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) { return false; }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return false; }

    // --- Balance ---

    @Override
    public double getBalance(String playerName) {
        // 非推奨: 名前からUUIDを引けないため0を返す
        return 0;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return cache.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) { return getBalance(playerName); }
    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override
    public boolean has(String playerName, double amount) { return false; }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return cache.getBalance(player.getUniqueId()) >= (long) amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    // --- Withdraw ---

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "名前ベース非推奨");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        long amt = (long) amount;
        UUID uuid = player.getUniqueId();
        if (amt <= 0) return new EconomyResponse(0, cache.getBalance(uuid), EconomyResponse.ResponseType.FAILURE, "金額は正の整数である必要があります");
        if (!cache.subtractBalance(uuid, amt)) {
            return new EconomyResponse(0, cache.getBalance(uuid), EconomyResponse.ResponseType.FAILURE, "残高不足");
        }
        plugin.getLogger().info("[Economy] withdraw " + player.getName() + " " + amt + " Cr");
        return new EconomyResponse(amt, cache.getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }

    // --- Deposit ---

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "名前ベース非推奨");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        long amt = (long) amount;
        UUID uuid = player.getUniqueId();
        if (amt <= 0) return new EconomyResponse(0, cache.getBalance(uuid), EconomyResponse.ResponseType.FAILURE, "金額は正の整数である必要があります");
        cache.addBalance(uuid, amt);
        plugin.getLogger().info("[Economy] deposit " + player.getName() + " " + amt + " Cr");
        return new EconomyResponse(amt, cache.getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    // --- Bank (未サポート) ---

    @Override public EconomyResponse createBank(String name, String player) { return notSupported(); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return notSupported(); }
    @Override public EconomyResponse deleteBank(String name) { return notSupported(); }
    @Override public EconomyResponse bankBalance(String name) { return notSupported(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return notSupported(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return notSupported(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return notSupported(); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return notSupported(); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return notSupported(); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return notSupported(); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return notSupported(); }
    @Override public List<String> getBanks() { return List.of(); }

    private EconomyResponse notSupported() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "バンク機能は非サポート");
    }
}
