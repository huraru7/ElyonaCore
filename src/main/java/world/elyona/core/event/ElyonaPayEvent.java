package world.elyona.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ElyonaPayEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player from;
    private final Player to;
    private final long amount;
    private final long tax;

    public ElyonaPayEvent(Player from, Player to, long amount, long tax) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.tax = tax;
    }

    public Player getFrom() { return from; }
    public Player getTo() { return to; }
    public long getAmount() { return amount; }
    public long getTax() { return tax; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
