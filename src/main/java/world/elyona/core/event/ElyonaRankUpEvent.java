package world.elyona.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import world.elyona.core.rank.RankTier;

public class ElyonaRankUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final RankTier oldRank;
    private final RankTier newRank;

    public ElyonaRankUpEvent(Player player, RankTier oldRank, RankTier newRank) {
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public Player getPlayer() { return player; }
    public RankTier getOldRank() { return oldRank; }
    public RankTier getNewRank() { return newRank; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
