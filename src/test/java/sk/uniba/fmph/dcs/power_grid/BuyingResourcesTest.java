package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BuyingResourcesTest {
    private Map<PlayerColor, PlayerBoardPort> players;
    private BuyingResources phase;

    @Before
    public void setUp() {
        players = PhaseTestDoubles.players(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE);
        phase = new BuyingResources(players);
    }

    private PhaseTestDoubles.Board board(final PlayerColor color) {
        return (PhaseTestDoubles.Board) players.get(color);
    }

    @Test
    public void purchasesFollowReverseOrderAndFinishAfterTheLastPlayer() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        List<Resource> resources = List.of(Resource.COAL, Resource.COAL, Resource.OIL);

        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), phase.buyResources(PlayerColor.BLUE, resources));
        assertEquals(resources, board(PlayerColor.BLUE).boughtResources);
        assertEquals(1, board(PlayerColor.BLUE).resourceCalls);
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyResources(PlayerColor.YELLOW, List.of(Resource.URANIUM)));
        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS),
                phase.buyResources(PlayerColor.RED, List.of(Resource.GARBAGE)));
    }

    @Test
    public void rejectsRequestsBeforeStartOutOfTurnUnknownRepeatedAndAfterFinish() {
        assertTrue(phase.buyResources(PlayerColor.BLUE, List.of(Resource.COAL)) instanceof Variant.Failure);
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        String before = phase.getState();
        assertTrue(phase.buyResources(PlayerColor.RED, List.of(Resource.COAL)) instanceof Variant.Failure);
        assertTrue(phase.buyResources(PlayerColor.BLACK, List.of(Resource.COAL)) instanceof Variant.Failure);
        assertEquals(before, phase.getState());
        assertEquals(0, board(PlayerColor.RED).resourceCalls);

        phase.buyResources(PlayerColor.BLUE, List.of());
        assertTrue(phase.buyResources(PlayerColor.BLUE, List.of(Resource.COAL)) instanceof Variant.Failure);
        phase.buyResources(PlayerColor.YELLOW, List.of());
        phase.buyResources(PlayerColor.RED, List.of());
        assertTrue(phase.buyResources(PlayerColor.RED, List.of(Resource.COAL)) instanceof Variant.Failure);
        assertEquals(0, board(PlayerColor.RED).resourceCalls);
        assertEquals(0, board(PlayerColor.BLUE).resourceCalls);
    }

    @Test
    public void failedPurchaseReturnsTheOriginalErrorAndAllowsRetry() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        RequestError error = new RequestError("Not enough money or storage space.");
        board(PlayerColor.BLUE).resourceError = Optional.of(error);
        String before = phase.getState();

        assertEquals(Variant.failure(error), phase.buyResources(PlayerColor.BLUE, List.of(Resource.OIL)));
        assertEquals(before, phase.getState());
        board(PlayerColor.BLUE).resourceError = Optional.empty();
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyResources(PlayerColor.BLUE, List.of(Resource.OIL)));
        assertEquals(2, board(PlayerColor.BLUE).resourceCalls);
    }

    @Test
    public void failedFinalPurchaseDoesNotEndThePhase() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        phase.buyResources(PlayerColor.BLUE, List.of());
        phase.buyResources(PlayerColor.YELLOW, List.of());
        board(PlayerColor.RED).resourceError = Optional.of(new RequestError("Resource unavailable."));

        assertTrue(phase.buyResources(PlayerColor.RED, List.of(Resource.COAL)) instanceof Variant.Failure);
        assertFalse(new JSONObject(phase.getState()).getBoolean("finished"));
        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS), phase.buyResources(PlayerColor.RED, List.of()));
    }

    @Test
    public void emptyPurchasePassesWithoutCallingTheBoard() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), phase.buyResources(PlayerColor.BLUE, List.of()));
        assertEquals(0, board(PlayerColor.BLUE).resourceCalls);
    }

    @Test
    public void stateReportsReverseOrderCurrentPlayerAndPhaseCompletion() {
        assertTrue(new JSONObject(phase.getState()).isNull("currentPlayer"));
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        JSONObject state = new JSONObject(phase.getState());
        assertEquals(List.of("BLUE", "YELLOW", "RED"), state.getJSONArray("playerOrder").toList());
        assertEquals("BLUE", state.getString("currentPlayer"));
        assertFalse(state.getBoolean("finished"));
        phase.buyResources(PlayerColor.BLUE, List.of());
        assertEquals("YELLOW", new JSONObject(phase.getState()).getString("currentPlayer"));
        phase.buyResources(PlayerColor.YELLOW, List.of());
        phase.buyResources(PlayerColor.RED, List.of());
        state = new JSONObject(phase.getState());
        assertTrue(state.isNull("currentPlayer"));
        assertTrue(state.getBoolean("finished"));
    }

    @Test
    public void nextRoundUsesNewOrderAndDoesNotModifyTheCallersList() {
        List<PlayerColor> order = new ArrayList<>(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        phase.startPhase(order);
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE), order);
        order.clear();
        phase.buyResources(PlayerColor.BLUE, List.of());
        phase.buyResources(PlayerColor.YELLOW, List.of());
        phase.buyResources(PlayerColor.RED, List.of());
        phase.startPhase(List.of(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.YELLOW));
        assertEquals("YELLOW", new JSONObject(phase.getState()).getString("currentPlayer"));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), phase.buyResources(PlayerColor.YELLOW, List.of()));
    }

    @Test
    public void invalidPlayerOrdersAreRejectedWithoutChangingThePhase() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        String before = phase.getState();
        for (List<PlayerColor> order : List.of(List.<PlayerColor>of(), List.of(PlayerColor.RED, PlayerColor.BLUE),
                List.of(PlayerColor.RED, PlayerColor.RED, PlayerColor.BLUE),
                List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLACK))) {
            assertThrows(IllegalArgumentException.class, () -> phase.startPhase(order));
            assertEquals(before, phase.getState());
        }
    }

    @Test
    public void playerMapIsCopiedAndInvalidPlayerCountsAreRejected() {
        players.clear();
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.buyResources(PlayerColor.BLUE, List.of(Resource.COAL)));
        assertThrows(IllegalArgumentException.class, () -> new BuyingResources(Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BuyingResources(PhaseTestDoubles.players(PlayerColor.RED)));
    }
}
