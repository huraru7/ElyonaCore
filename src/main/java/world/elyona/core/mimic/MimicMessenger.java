package world.elyona.core.mimic;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.elyona.core.config.PluginConfig;

public class MimicMessenger {

    private final PluginConfig config;

    public MimicMessenger(PluginConfig config) {
        this.config = config;
    }

    /**
     * MIMICフォーマットでメッセージComponentを構築する
     * 形式: [MIMIC] 「{message}」
     */
    public Component build(String message) {
        TextColor prefixColor = TextColor.fromHexString(config.getMimicPrefixColor());
        TextColor textColor = TextColor.fromHexString(config.getMimicTextColor());

        return Component.text("[MIMIC] ", prefixColor)
                .append(Component.text("「" + message + "」", textColor));
    }

    /** 特定プレイヤーにMIMICメッセージを送信 */
    public void sendTo(Player player, String message) {
        ((Audience) player).sendMessage(build(message));
    }

    /** 全プレイヤーにMIMICメッセージをブロードキャスト */
    public void broadcast(String message) {
        Component component = build(message);
        Bukkit.getOnlinePlayers().forEach(p -> ((Audience) p).sendMessage(component));
    }

    /** 特定ワールドの全プレイヤーにMIMICメッセージを送信 */
    public void broadcastWorld(World world, String message) {
        Component component = build(message);
        world.getPlayers().forEach(p -> ((Audience) p).sendMessage(component));
    }
}
