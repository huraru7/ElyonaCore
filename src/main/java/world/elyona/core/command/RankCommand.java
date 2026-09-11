package world.elyona.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.elyona.core.rank.RankGui;
import world.elyona.core.rank.RankManager;
import world.elyona.core.rank.RankTier;
import world.elyona.core.rank.SeasonManager;

public class RankCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final SeasonManager seasonManager;

    public RankCommand(RankManager rankManager, SeasonManager seasonManager) {
        this.rankManager = rankManager;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        if (!seasonManager.hasActiveSeason()) {
            player.sendMessage("現在アクティブなシーズンはありません。");
            return true;
        }

        if (args.length == 0) {
            // 自分のランクをGUIで表示
            RankTier tier = rankManager.getRank(player);
            long exp = rankManager.getExp(player);
            new RankGui(player, tier, exp).open(player);
        } else {
            // 他プレイヤーのランクを確認
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage("オンラインプレイヤーが見つかりません: " + args[0]);
                return true;
            }
            RankTier tier = rankManager.getRank(target);
            long exp = rankManager.getExp(target);
            player.sendMessage(target.getName() + " のランク: " + tier.displayName + " (EXP: " + exp + ")");
        }
        return true;
    }
}
