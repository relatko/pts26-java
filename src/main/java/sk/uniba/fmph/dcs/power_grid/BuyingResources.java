package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles phase 3: each player may buy resources once, in reverse player order. */
public final class BuyingResources {
    private final Map<PlayerColor, PlayerBoardPort> players;
    private final ReversePlayerOrder turns;

    public BuyingResources(final Map<PlayerColor, PlayerBoardPort> players) {
        this.players = Map.copyOf(players);
        turns = new ReversePlayerOrder(this.players.keySet());
    }

    /**
     * Starts a new phase using the normal (first-to-last) round order.
     *
     * @param playerOrder every participating player exactly once, in normal round order
     */
    public void startPhase(final List<PlayerColor> playerOrder) {
        turns.startPhase(playerOrder);
    }

    /**
     * An empty resource list passes the turn; a failed purchase keeps the turn.
     *
     * @param player the player submitting the purchase
     * @param resources all resource tokens to purchase this turn; empty to pass
     * @return the phase status on success, or the purchase/turn error
     */
    public Variant<PhaseStatus, RequestError> buyResources(final PlayerColor player,
            final List<Resource> resources) {
        if (!turns.isCurrentPlayer(player)) {
            return Variant.failure(new RequestError("It is not this player's turn to buy resources."));
        }
        if (!resources.isEmpty()) {
            Optional<RequestError> error = players.get(player).buyResources(List.copyOf(resources));
            if (error.isPresent()) {
                return Variant.failure(error.get());
            }
        }
        return Variant.success(turns.finishTurn());
    }

    /**
     * Returns the phase's turn state.
     *
     * @return JSON with playerOrder, currentPlayer (null when inactive), and finished
     */
    public String getState() {
        return turns.getState();
    }
}
