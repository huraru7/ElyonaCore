package world.elyona.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ElyonaTitleGrantEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String titleId;

    public ElyonaTitleGrantEvent(Player player, String titleId) {
        this.player = player;
        this.titleId = titleId;
    }

    public Player getPlayer() { return player; }
    public String getTitleId() { return titleId; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
