package sk.uniba.fmph.dcs.power_grid;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

/** Handles normal-round electricity production, income, and end-of-round market updates. */
public final class Bureaucracy {
    private final TownsPort towns;
    private final ResourceMarket resourceMarket;
    private final PowerPlantMarketPort powerPlantMarket;
    private final Map<PlayerColor, PlayerBoardPort> players;
    private List<PlayerColor> playerOrder = List.of();
    private int nextPlayer;

    public Bureaucracy(final TownsPort towns, final ResourceMarket resourceMarket,
            final PowerPlantMarketPort powerPlantMarket, final Map<PlayerColor, PlayerBoardPort> players) {
        this.towns = Objects.requireNonNull(towns);
        this.resourceMarket = Objects.requireNonNull(resourceMarket);
        this.powerPlantMarket = Objects.requireNonNull(powerPlantMarket);
        this.players = Map.copyOf(players);
        if (this.players.size() < 2 || this.players.size() > 6) {
            throw new IllegalArgumentException("The game requires 2 to 6 players.");
        }
    }

    /**
     * Starts a new phase in normal (first-to-last) round order.
     *
     * @param playerOrder every participating player exactly once, in normal round order
     */
    public void startPhase(final List<PlayerColor> playerOrder) {
        List<PlayerColor> copy = List.copyOf(playerOrder);
        if (copy.size() != players.size() || !new HashSet<>(copy).equals(players.keySet())) {
            throw new IllegalArgumentException("Player order must contain every player exactly once.");
        }
        this.playerOrder = copy;
        nextPlayer = 0;
    }

    /**
     * Runs the selected plants and pays for the supplied cities in the player's network.
     * Empty selections still go through the board so that zero production earns the minimum income.
     * A board error leaves the turn unchanged and does not pay income or update markets.
     *
     * @param player the player producing electricity
     * @param ppns the selected power plant numbers; may be empty
     * @param resourcesToBurn the resource tokens used by the selected plants; may be empty
     * @return the phase status on success, or the production/turn error
     */
    public Variant<PhaseStatus, RequestError> usePowerPlants(final PlayerColor player,
            final List<PowerPlantNumber> ppns, final List<Resource> resourcesToBurn) {
        if (nextPlayer == playerOrder.size() || playerOrder.get(nextPlayer) != player) {
            return Variant.failure(new RequestError("It is not this player's turn to use power plants."));
        }

        PlayerBoardPort board = players.get(player);
        Variant<Integer, RequestError> production = board.usePowerPlants(List.copyOf(ppns),
                List.copyOf(resourcesToBurn));
        if (production instanceof Variant.Failure<Integer, RequestError> failure) {
            return Variant.failure(failure.error());
        }
        int suppliedTowns = ((Variant.Success<Integer, RequestError>) production).value();
        board.receiveMoney(Math.min(suppliedTowns, towns.getNumberOfHouses(player)));
        nextPlayer++;

        if (nextPlayer == playerOrder.size()) {
            resourceMarket.refill();
            powerPlantMarket.prepareNextTurn();
            return Variant.success(PhaseStatus.PHASE_ENDS);
        }
        return Variant.success(PhaseStatus.PHASE_CONTINUES);
    }

    /**
     * Returns the phase's turn state.
     *
     * @return JSON with playerOrder in execution order, currentPlayer (null when inactive), and finished
     */
    public String getState() {
        boolean finished = nextPlayer == playerOrder.size();
        return new JSONObject()
                .put("playerOrder", new JSONArray(playerOrder))
                .put("currentPlayer", finished ? JSONObject.NULL : playerOrder.get(nextPlayer).name())
                .put("finished", finished)
                .toString();
    }
}
