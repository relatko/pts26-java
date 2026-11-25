package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BuildingHousesTest {
    private Map<PlayerColor, PlayerBoardPort> players;
    private PhaseTestDoubles.Towns towns;
    private PhaseTestDoubles.Market market;
    private BuildingHouses phase;

    @Before
    public void setUp() {
        players = PhaseTestDoubles.players(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE);
        towns = new PhaseTestDoubles.Towns();
        market = new PhaseTestDoubles.Market();
        phase = new BuildingHouses(towns, market, players);
    }

    private PhaseTestDoubles.Board board(final PlayerColor color) {
        return (PhaseTestDoubles.Board) players.get(color);
    }

    @Test
    public void delegatesConstructionWithCurrentBalanceAndChargesReturnedCost() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        board(PlayerColor.BLUE).money = 41;
        towns.cost = 27;
        List<String> townNames = List.of("Essen", "Duisburg");

        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), phase.buyHouses(PlayerColor.BLUE, townNames));
        assertEquals(PlayerColor.BLUE, towns.receivedPlayer);
        assertEquals(townNames, towns.receivedTownNames);
        assertEquals(41, towns.receivedMaxMoney);
        assertEquals(14, board(PlayerColor.BLUE).money);
        assertEquals(1, board(PlayerColor.BLUE).spendCalls);
        assertEquals(1, towns.buyCalls);
        assertEquals(50, board(PlayerColor.YELLOW).money);
        assertEquals(50, board(PlayerColor.RED).money);
    }

    @Test
    public void successfulBuildingUpdatesMarketUsingTheLargestNetwork() {
        towns.houses.put(PlayerColor.RED, 9);
        towns.houses.put(PlayerColor.BLUE, 8);
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        phase.buyHouses(PlayerColor.BLUE, List.of("Essen", "Duisburg"));
        assertEquals(List.of(10), market.cleanupLimits);
        phase.buyHouses(PlayerColor.YELLOW, List.of("Berlin"));
        assertEquals(List.of(10, 10), market.cleanupLimits);
    }

    @Test
    public void phasesRunInReverseOrderAndOnlyFinishAfterAllPlayersAct() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyHouses(PlayerColor.BLUE, List.of("Essen")));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyHouses(PlayerColor.YELLOW, List.of("Berlin")));
        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS),
                phase.buyHouses(PlayerColor.RED, List.of("Hannover")));
    }

    @Test
    public void rejectsRequestsBeforeStartOutOfTurnUnknownRepeatedAndAfterFinish() {
        assertTrue(phase.buyHouses(PlayerColor.BLUE, List.of("Essen")) instanceof Variant.Failure);
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        String before = phase.getState();
        assertTrue(phase.buyHouses(PlayerColor.RED, List.of("Essen")) instanceof Variant.Failure);
        assertTrue(phase.buyHouses(PlayerColor.BLACK, List.of("Essen")) instanceof Variant.Failure);
        assertEquals(before, phase.getState());
        phase.buyHouses(PlayerColor.BLUE, List.of());
        assertTrue(phase.buyHouses(PlayerColor.BLUE, List.of("Essen")) instanceof Variant.Failure);
        phase.buyHouses(PlayerColor.YELLOW, List.of());
        phase.buyHouses(PlayerColor.RED, List.of());
        assertTrue(phase.buyHouses(PlayerColor.RED, List.of("Essen")) instanceof Variant.Failure);
        assertEquals(0, towns.buyCalls);
        assertTrue(market.cleanupLimits.isEmpty());
        for (PlayerBoardPort player : players.values()) {
            assertEquals(0, ((PhaseTestDoubles.Board) player).spendCalls);
        }
    }

    @Test
    public void failedConstructionPreservesTurnMoneyAndMarketAndAllowsRetry() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        towns.error = new RequestError("Cannot build in this city.");
        String before = phase.getState();

        assertEquals(Variant.failure(towns.error), phase.buyHouses(PlayerColor.BLUE, List.of("Essen")));
        assertEquals(before, phase.getState());
        assertEquals(50, board(PlayerColor.BLUE).money);
        assertEquals(0, board(PlayerColor.BLUE).spendCalls);
        assertEquals(0, towns.maxHousesCalls);
        assertTrue(market.cleanupLimits.isEmpty());
        towns.error = null;
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyHouses(PlayerColor.BLUE, List.of("Essen")));
        assertEquals(1, board(PlayerColor.BLUE).spendCalls);
        assertEquals(2, towns.buyCalls);
    }

    @Test
    public void passingDoesNotBuildChargeMoneyOrUpdateTheMarket() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), phase.buyHouses(PlayerColor.BLUE, List.of()));
        assertEquals(0, towns.buyCalls);
        assertEquals(0, board(PlayerColor.BLUE).spendCalls);
        assertTrue(market.cleanupLimits.isEmpty());
    }

    @Test
    public void reachingThresholdEarlyStillLetsEveryOtherPlayerBuild() {
        towns.houses.put(PlayerColor.BLUE, 16);
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyHouses(PlayerColor.BLUE, List.of("Essen")));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyHouses(PlayerColor.YELLOW, List.of("Berlin")));
        assertEquals(Variant.success(PhaseStatus.GAME_ENDS), phase.buyHouses(PlayerColor.RED, List.of()));
        assertTrue(new JSONObject(phase.getState()).getBoolean("finished"));
    }

    @Test
    public void failedFinalPurchaseDoesNotFinishEvenWhenThresholdHasBeenReached() {
        towns.houses.put(PlayerColor.BLUE, 17);
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        phase.buyHouses(PlayerColor.BLUE, List.of());
        phase.buyHouses(PlayerColor.YELLOW, List.of());
        towns.error = new RequestError("Insufficient money.");
        assertEquals(Variant.failure(towns.error), phase.buyHouses(PlayerColor.RED, List.of("Berlin")));
        assertFalse(new JSONObject(phase.getState()).getBoolean("finished"));
        assertEquals(0, board(PlayerColor.RED).spendCalls);
        assertEquals(Variant.success(PhaseStatus.GAME_ENDS), phase.buyHouses(PlayerColor.RED, List.of()));
    }

    @Test
    public void gameEndThresholdsCoverTwoToSixPlayersIncludingExactAndExceededLimits() {
        int[] thresholds = {18, 17, 17, 15, 14};
        for (int count = 2; count <= 6; count++) {
            List<PlayerColor> order = List.of(PlayerColor.values()).subList(0, count);
            for (int delta = -1; delta <= 1; delta++) {
                Map<PlayerColor, PlayerBoardPort> boards = PhaseTestDoubles.players(order.toArray(PlayerColor[]::new));
                PhaseTestDoubles.Towns networks = new PhaseTestDoubles.Towns();
                int threshold = thresholds[count - 2];
                // The last player to act reaches (or exceeds) the limit with this purchase.
                networks.houses.put(order.getFirst(), threshold + delta - 1);
                BuildingHouses building = new BuildingHouses(networks, new PhaseTestDoubles.Market(), boards);
                building.startPhase(order);
                for (int i = count - 1; i > 0; i--) {
                    assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                            building.buyHouses(order.get(i), List.of()));
                }
                PhaseStatus expected = delta < 0 ? PhaseStatus.PHASE_ENDS : PhaseStatus.GAME_ENDS;
                assertEquals("Player count " + count + ", threshold delta " + delta,
                        Variant.success(expected), building.buyHouses(order.getFirst(), List.of("Essen")));
            }
        }
    }

    @Test
    public void stateReportsReverseOrderCurrentPlayerAndCompletion() {
        assertTrue(new JSONObject(phase.getState()).isNull("currentPlayer"));
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        JSONObject state = new JSONObject(phase.getState());
        assertEquals(List.of("BLUE", "YELLOW", "RED"), state.getJSONArray("playerOrder").toList());
        assertEquals("BLUE", state.getString("currentPlayer"));
        phase.buyHouses(PlayerColor.BLUE, List.of());
        assertEquals("YELLOW", new JSONObject(phase.getState()).getString("currentPlayer"));
        phase.buyHouses(PlayerColor.YELLOW, List.of());
        phase.buyHouses(PlayerColor.RED, List.of());
        state = new JSONObject(phase.getState());
        assertTrue(state.isNull("currentPlayer"));
        assertTrue(state.getBoolean("finished"));
    }

    @Test
    public void nextRoundUsesNewOrderAndCopiesTheCallersList() {
        List<PlayerColor> order = new ArrayList<>(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        phase.startPhase(order);
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE), order);
        order.clear();
        phase.buyHouses(PlayerColor.BLUE, List.of());
        phase.buyHouses(PlayerColor.YELLOW, List.of());
        phase.buyHouses(PlayerColor.RED, List.of());
        phase.startPhase(List.of(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.YELLOW));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), phase.buyHouses(PlayerColor.YELLOW, List.of()));
        assertEquals("RED", new JSONObject(phase.getState()).getString("currentPlayer"));
    }

    @Test
    public void invalidOrdersAndPlayerCountsAreRejected() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        String before = phase.getState();
        assertThrows(IllegalArgumentException.class, () -> phase.startPhase(List.of(PlayerColor.RED, PlayerColor.BLUE)));
        assertThrows(IllegalArgumentException.class,
                () -> phase.startPhase(List.of(PlayerColor.RED, PlayerColor.RED, PlayerColor.BLUE)));
        assertThrows(IllegalArgumentException.class,
                () -> phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLACK)));
        assertEquals(before, phase.getState());
        assertThrows(IllegalArgumentException.class, () -> new BuildingHouses(towns, market, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingHouses(towns, market, PhaseTestDoubles.players(PlayerColor.RED)));
    }
}
