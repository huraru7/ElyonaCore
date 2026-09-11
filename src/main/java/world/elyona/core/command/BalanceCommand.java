package world.elyona.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.elyona.core.config.PluginConfig;
import world.elyona.core.economy.EconomyCache;

public class BalanceCommand implements CommandExecutor {

    private final EconomyCache cache;
    private final PluginConfig config;

    public BalanceCommand(EconomyCache cache, PluginConfig config) {
        this.cache = cache;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        long balance = cache.getBalance(player.getUniqueId());
        player.sendMessage(config.getCurrencySymbol() + " " + balance + " " + config.getCurrencyName());
        return true;
    }
}
