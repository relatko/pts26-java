package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Handles phase 4: building and payment in reverse player order. */
public final class BuildingHouses {
    private final TownsPort towns;
    private final PowerPlantMarketPort market;
    private final Map<PlayerColor, PlayerBoardPort> players;
    private final ReversePlayerOrder turns;
    private final int gameEndThreshold;

    public BuildingHouses(final TownsPort towns, final PowerPlantMarketPort market,
            final Map<PlayerColor, PlayerBoardPort> players) {
        this.towns = Objects.requireNonNull(towns);
        this.market = Objects.requireNonNull(market);
        this.players = Map.copyOf(players);
        turns = new ReversePlayerOrder(this.players.keySet());
        gameEndThreshold = switch (this.players.size()) {
            case 2 -> 18;
            case 3, 4 -> 17;
            case 5 -> 15;
            case 6 -> 14;
            default -> throw new IllegalArgumentException("The game requires 2 to 6 players.");
        };
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
     * An empty town list passes the turn. A failed purchase keeps the turn and does not charge money.
     * The game-end threshold is checked only after the last player's successful turn or pass.
     *
     * @param player the player submitting the purchase
     * @param townNames all towns to connect this turn; empty to pass
     * @return the phase status on success, or the construction/turn error
     */
    public Variant<PhaseStatus, RequestError> buyHouses(final PlayerColor player,
            final List<String> townNames) {
        if (!turns.isCurrentPlayer(player)) {
            return Variant.failure(new RequestError("It is not this player's turn to build houses."));
        }
        if (!townNames.isEmpty()) {
            PlayerBoardPort board = players.get(player);
            Variant<Integer, RequestError> purchase = towns.buyHouses(player, List.copyOf(townNames),
                    board.outstandingMoney());
            if (purchase instanceof Variant.Failure<Integer, RequestError> failure) {
                return Variant.failure(failure.error());
            }
            int cost = ((Variant.Success<Integer, RequestError>) purchase).value();
            board.spendMoney(cost);
            // Follow the UML's market cleanup rule through the shared market interface.
            market.removePowerPlantsCheaperThan(towns.getMaxNumberOfHouses());
        }
        PhaseStatus status = turns.finishTurn();
        if (status == PhaseStatus.PHASE_ENDS && towns.getMaxNumberOfHouses() >= gameEndThreshold) {
            status = PhaseStatus.GAME_ENDS;
        }
        return Variant.success(status);
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
