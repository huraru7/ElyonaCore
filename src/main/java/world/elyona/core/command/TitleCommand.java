package world.elyona.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.core.title.TitleGui;
import world.elyona.core.title.TitleManager;

public class TitleCommand implements CommandExecutor {

    private final TitleManager titleManager;
    private final MimicMessenger mimic;

    public TitleCommand(TitleManager titleManager, MimicMessenger mimic) {
        this.titleManager = titleManager;
        this.mimic = mimic;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        if (args.length == 0) {
            // GUI表示
            openTitleGui(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 2) { mimic.sendTo(player, "使用方法: /title set <称号ID>"); return true; }
                titleManager.setActive(player, args[1]);
            }
            case "clear" -> titleManager.clearActive(player);
            case "grant" -> {
                if (!player.hasPermission("elyona.admin")) {
                    mimic.sendTo(player, "権限がありません。");
                    return true;
                }
                if (args.length < 3) { mimic.sendTo(player, "使用方法: /title grant <player> <称号ID>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { mimic.sendTo(player, "プレイヤーが見つかりません: " + args[1]); return true; }
                titleManager.grant(target, args[2]);
                mimic.sendTo(player, target.getName() + " に称号「" + args[2] + "」を付与しました。");
            }
            default -> mimic.sendTo(player, "使用方法: /title [set <id>|clear|grant <player> <id>]");
        }
        return true;
    }

    private void openTitleGui(Player player) {
        titleManager.getOwnedTitles(player).thenAccept(ownedIds -> {
            String activeId = titleManager.getActive(player);
            Bukkit.getScheduler().runTask(
                    world.elyona.core.ElyonaCorePlugin.getInstance(),
                    () -> new TitleGui(player, ownedIds, world.elyona.core.ElyonaCorePlugin.getInstance().getTitleLoader(), activeId).open()
            );
        });
    }
}
