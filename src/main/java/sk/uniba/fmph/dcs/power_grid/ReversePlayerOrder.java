package sk.uniba.fmph.dcs.power_grid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/** Tracks one turn per player in the resource-buying and house-building phases. */
final class ReversePlayerOrder {
    private final Set<PlayerColor> players;
    private List<PlayerColor> playerOrder = List.of();
    private int nextPlayer;

    ReversePlayerOrder(final Set<PlayerColor> players) {
        this.players = Set.copyOf(players);
        if (players.size() < 2 || players.size() > 6) {
            throw new IllegalArgumentException("The game requires 2 to 6 players.");
        }
    }

    void startPhase(final List<PlayerColor> order) {
        List<PlayerColor> copy = List.copyOf(order);
        if (copy.size() != players.size() || !new HashSet<>(copy).equals(players)) {
            throw new IllegalArgumentException("Player order must contain every player exactly once.");
        }
        playerOrder = copy.reversed();
        nextPlayer = 0;
    }

    boolean isCurrentPlayer(final PlayerColor player) {
        return !isFinished() && playerOrder.get(nextPlayer) == player;
    }

    boolean isFinished() {
        return nextPlayer == playerOrder.size();
    }

    PhaseStatus finishTurn() {
        nextPlayer++;
        return isFinished() ? PhaseStatus.PHASE_ENDS : PhaseStatus.PHASE_CONTINUES;
    }

    String getState() {
        return new JSONObject()
                .put("playerOrder", new JSONArray(playerOrder))
                .put("currentPlayer", isFinished() ? JSONObject.NULL : playerOrder.get(nextPlayer).name())
                .put("finished", isFinished())
                .toString();
    }
}
