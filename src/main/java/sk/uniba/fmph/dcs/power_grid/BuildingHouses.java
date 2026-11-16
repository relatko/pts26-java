package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * BuildingHouses manages Phase 4 (Build Houses) of Power Grid.
 * Player order (weakest to strongest) is passed to {@link #startPhase(List)}.
 * Building occurs in reverse player order (strongest first).
 * Once all players finish their turns, returns {@link PhaseStatus#GAME_ENDS} if
 * any player
 * reached the end-game city threshold, or {@link PhaseStatus#PHASE_ENDS}
 * otherwise.
 */
public final class BuildingHouses {
    private static final int END_GAME_CITIES_2_PLAYERS = 18;
    private static final int END_GAME_CITIES_3_4_PLAYERS = 17;
    private static final int END_GAME_CITIES_5_PLAYERS = 15;
    private static final int END_GAME_CITIES_6_PLAYERS = 14;

    private final Map<PlayerColor, InterfacePlayerBoardFacade> playerFacades;
    private final InterfaceTownsBuildingHouses towns;
    private final Queue<PlayerColor> buildingOrder = new LinkedList<>();
    private boolean phaseActive = false;

    public BuildingHouses(final Map<PlayerColor, InterfacePlayerBoardFacade> playerFacades,
            final InterfaceTownsBuildingHouses towns) {
        this.playerFacades = Objects.requireNonNull(playerFacades, "playerFacades must not be null");
        this.towns = Objects.requireNonNull(towns, "towns must not be null");
    }

    /**
     * Starts the building phase. {@code playerOrder} is ordered from weakest to
     * strongest;
     * building proceeds in reverse order (strongest first).
     *
     * @param playerOrder current round player order (weakest to strongest)
     */
    public void startPhase(final List<PlayerColor> playerOrder) {
        Objects.requireNonNull(playerOrder, "playerOrder must not be null");
        if (playerOrder.isEmpty()) {
            throw new IllegalArgumentException("playerOrder must not be empty");
        }
        buildingOrder.clear();
        List<PlayerColor> reversed = new ArrayList<>(playerOrder);
        Collections.reverse(reversed);
        buildingOrder.addAll(reversed);
        phaseActive = true;
    }

    /**
     * Executes a player's house-building turn for the specified towns.
     *
     * @param player    the player acting
     * @param townNames list of town names to connect (may be empty)
     * @return {@link PhaseStatus} on success, or {@link RequestError} on failure
     */
    public Object buyHouses(final PlayerColor player, final List<String> townNames) {
        if (player == null || townNames == null) {
            return new RequestError("Player and townNames must not be null.");
        }
        if (!phaseActive) {
            return new RequestError("Building phase is not active.");
        }
        PlayerColor expected = buildingOrder.peek();
        if (expected == null) {
            return new RequestError("All players have already acted this phase.");
        }
        if (expected != player) {
            return new RequestError("It is not " + player + "'s turn to build. Expected: " + expected + ".");
        }

        InterfacePlayerBoardFacade facade = playerFacades.get(player);
        if (facade == null) {
            return new RequestError("No facade found for player " + player + ".");
        }

        Object result = towns.buyHouses(player, townNames);
        if (result instanceof RequestError) {
            return result;
        }

        int cost = (Integer) result;
        if (cost < 0) {
            return new RequestError("Invalid negative building cost.");
        }
        if (!facade.hasEnoughMoney(cost)) {
            return new RequestError("Player " + player + " does not have enough money.");
        }

        facade.spendMoney(cost);
        buildingOrder.poll();

        if (buildingOrder.isEmpty()) {
            phaseActive = false;
            if (isGameEndReached()) {
                return PhaseStatus.GAME_ENDS;
            }
            return PhaseStatus.PHASE_ENDS;
        }
        return PhaseStatus.PHASE_CONTINUES;
    }

    public String getState() {
        StringBuilder orderJson = new StringBuilder("[");
        int idx = 0;
        for (PlayerColor color : buildingOrder) {
            if (idx > 0) {
                orderJson.append(",");
            }
            orderJson.append("\"").append(color.name()).append("\"");
            idx++;
        }
        orderJson.append("]");
        return "{\"phaseActive\":" + phaseActive + ",\"buildingOrder\":" + orderJson + "}";
    }

    @Override
    public String toString() {
        return getState();
    }

    private boolean isGameEndReached() {
        int threshold = endGameCityThreshold(playerFacades.size());
        for (InterfacePlayerBoardFacade facade : playerFacades.values()) {
            if (facade.numberOfConnectedCities() >= threshold) {
                return true;
            }
        }
        return false;
    }

    private static int endGameCityThreshold(final int playerCount) {
        if (playerCount <= 2) {
            return END_GAME_CITIES_2_PLAYERS;
        }
        if (playerCount <= 4) {
            return END_GAME_CITIES_3_4_PLAYERS;
        }
        if (playerCount == 5) {
            return END_GAME_CITIES_5_PLAYERS;
        }
        return END_GAME_CITIES_6_PLAYERS;
    }
}
