package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BureaucracyTest {
    private final List<String> events = new ArrayList<>();
    private Map<PlayerColor, PlayerBoardPort> players;
    private TownsFake towns;
    private PoolFake pool;
    private ResourceMarket resourceMarket;
    private MarketFake powerPlantMarket;
    private Bureaucracy phase;

    @Before
    public void setUp() {
        players = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color : List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE)) {
            players.put(color, new BoardFake(color));
        }
        towns = new TownsFake();
        pool = new PoolFake();
        Map<Integer, Map<Resource, Integer>> refillPattern = Map.of(
                1, Map.of(Resource.COAL, 1, Resource.OIL, 1, Resource.GARBAGE, 1, Resource.URANIUM, 1),
                2, Map.of(Resource.COAL, 2, Resource.OIL, 2, Resource.GARBAGE, 2, Resource.URANIUM, 2),
                3, Map.of(Resource.COAL, 3, Resource.OIL, 3, Resource.GARBAGE, 3, Resource.URANIUM, 3));
        resourceMarket = new ResourceMarket(pool, List.of(), refillPattern);
        powerPlantMarket = new MarketFake();
        phase = new Bureaucracy(towns, resourceMarket, powerPlantMarket, players);
    }

    private BoardFake board(final PlayerColor player) {
        return (BoardFake) players.get(player);
    }

    private void startPhase() {
        phase.startPhase(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
    }

    private Variant<PhaseStatus, RequestError> produce(final PlayerColor player) {
        return phase.usePowerPlants(player, List.of(new PowerPlantNumber(7)), List.of(Resource.OIL, Resource.OIL));
    }

    @Test
    public void productionFollowsTheSuppliedForwardOrder() {
        phase.startPhase(List.of(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.YELLOW));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), produce(PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), produce(PlayerColor.RED));
        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS), produce(PlayerColor.YELLOW));
        assertEquals(List.of("use:BLUE", "pay:BLUE", "use:RED", "pay:RED", "use:YELLOW", "pay:YELLOW"),
                events.subList(0, 6));
    }

    @Test
    public void selectedPlantsAndResourcesAreDelegatedAndIncomeIsCappedByBuiltHouses() {
        startPhase();
        List<PowerPlantNumber> ppns = List.of(new PowerPlantNumber(7), new PowerPlantNumber(10));
        List<Resource> resources = List.of(Resource.OIL, Resource.OIL, Resource.COAL, Resource.COAL);
        board(PlayerColor.RED).production = Variant.success(7);
        towns.houses.put(PlayerColor.RED, 6);

        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.usePowerPlants(PlayerColor.RED, ppns, resources));
        assertEquals(ppns, board(PlayerColor.RED).selectedPlants);
        assertEquals(resources, board(PlayerColor.RED).burnedResources);
        assertEquals(List.of(6), board(PlayerColor.RED).paidForTowns);

        board(PlayerColor.YELLOW).production = Variant.success(2);
        towns.houses.put(PlayerColor.YELLOW, 5);
        produce(PlayerColor.YELLOW);
        assertEquals(List.of(2), board(PlayerColor.YELLOW).paidForTowns);

        towns.houses.put(PlayerColor.BLUE, 0);
        produce(PlayerColor.BLUE);
        assertEquals(List.of(0), board(PlayerColor.BLUE).paidForTowns);
    }

    @Test
    public void emptySelectionStillUsesTheBoardAndRequestsMinimumIncome() {
        startPhase();
        board(PlayerColor.RED).production = Variant.success(0);
        towns.houses.put(PlayerColor.RED, 3);
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.usePowerPlants(PlayerColor.RED, List.of(), List.of()));
        assertEquals(1, board(PlayerColor.RED).productionCalls);
        assertTrue(board(PlayerColor.RED).selectedPlants.isEmpty());
        assertTrue(board(PlayerColor.RED).burnedResources.isEmpty());
        // PlayerBoardPort.receiveMoney takes a city count, not an amount of Elektro.
        assertEquals(List.of(0), board(PlayerColor.RED).paidForTowns);
    }

    @Test
    public void plantsWithoutResourcesCanProduceElectricity() {
        startPhase();
        board(PlayerColor.RED).production = Variant.success(1);
        towns.houses.put(PlayerColor.RED, 2);
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES),
                phase.usePowerPlants(PlayerColor.RED, List.of(new PowerPlantNumber(13)), List.of()));
        assertTrue(board(PlayerColor.RED).burnedResources.isEmpty());
        assertEquals(List.of(1), board(PlayerColor.RED).paidForTowns);
    }

    @Test
    public void productionErrorsPreserveTheTurnAndAllowRetryWithoutPaying() {
        startPhase();
        RequestError error = new RequestError("Not enough resources for the selected plants.");
        board(PlayerColor.RED).production = Variant.failure(error);
        String before = phase.getState();

        assertEquals(Variant.failure(error), produce(PlayerColor.RED));
        assertEquals(before, phase.getState());
        assertTrue(board(PlayerColor.RED).paidForTowns.isEmpty());
        assertTrue(pool.requests.isEmpty());
        assertEquals(0, powerPlantMarket.prepareCalls);

        board(PlayerColor.RED).production = Variant.success(2);
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), produce(PlayerColor.RED));
        assertEquals(2, board(PlayerColor.RED).productionCalls);
        assertEquals(List.of(2), board(PlayerColor.RED).paidForTowns);
    }

    @Test
    public void rejectsCallsBeforeStartOutOfTurnUnknownRepeatedAndAfterCompletion() {
        assertTrue(produce(PlayerColor.RED) instanceof Variant.Failure);
        startPhase();
        String before = phase.getState();
        assertTrue(produce(PlayerColor.BLUE) instanceof Variant.Failure);
        assertTrue(produce(PlayerColor.BLACK) instanceof Variant.Failure);
        assertEquals(before, phase.getState());
        assertTrue(events.isEmpty());

        produce(PlayerColor.RED);
        assertTrue(produce(PlayerColor.RED) instanceof Variant.Failure);
        produce(PlayerColor.YELLOW);
        produce(PlayerColor.BLUE);
        List<String> afterCompletion = List.copyOf(events);
        assertTrue(produce(PlayerColor.BLUE) instanceof Variant.Failure);
        assertTrue(produce(PlayerColor.RED) instanceof Variant.Failure);
        assertEquals(afterCompletion, events);
    }

    @Test
    public void finalPlayerErrorDoesNotRefillOrFinishAndCanBeRetried() {
        startPhase();
        produce(PlayerColor.RED);
        produce(PlayerColor.YELLOW);
        RequestError error = new RequestError("Invalid resource selection.");
        board(PlayerColor.BLUE).production = Variant.failure(error);

        assertEquals(Variant.failure(error), produce(PlayerColor.BLUE));
        assertEquals("BLUE", new JSONObject(phase.getState()).getString("currentPlayer"));
        assertFalse(new JSONObject(phase.getState()).getBoolean("finished"));
        assertTrue(board(PlayerColor.BLUE).paidForTowns.isEmpty());
        assertTrue(pool.requests.isEmpty());
        assertEquals(0, powerPlantMarket.prepareCalls);

        board(PlayerColor.BLUE).production = Variant.success(2);
        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS), produce(PlayerColor.BLUE));
        assertEquals(List.of(2), board(PlayerColor.BLUE).paidForTowns);
        assertEquals(4, pool.requests.size());
        assertEquals(1, powerPlantMarket.prepareCalls);
    }

    @Test
    public void marketsUpdateExactlyOnceAfterAllIncomeAndInTheRequiredOrder() {
        startPhase();
        produce(PlayerColor.RED);
        produce(PlayerColor.YELLOW);
        assertTrue(pool.requests.isEmpty());
        assertEquals(0, powerPlantMarket.prepareCalls);
        assertNull(resourceMarket.getPrice(List.of(Resource.COAL)));

        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS), produce(PlayerColor.BLUE));
        assertEquals(List.of("use:RED", "pay:RED", "use:YELLOW", "pay:YELLOW", "use:BLUE", "pay:BLUE",
                "refill:COAL", "refill:OIL", "refill:GARBAGE", "refill:URANIUM", "prepare"), events);
        assertEquals(Integer.valueOf(8), resourceMarket.getPrice(List.of(Resource.COAL)));
        assertEquals(1, powerPlantMarket.prepareCalls);

        assertTrue(produce(PlayerColor.BLUE) instanceof Variant.Failure);
        assertEquals(4, pool.requests.size());
        assertEquals(1, powerPlantMarket.prepareCalls);
    }

    @Test
    public void resourceRefillUsesTheMarketsCurrentStep() {
        resourceMarket.startStep2();
        resourceMarket.startStep3();
        startPhase();
        produce(PlayerColor.RED);
        produce(PlayerColor.YELLOW);
        produce(PlayerColor.BLUE);
        assertEquals(List.of(List.of(Resource.COAL, Resource.COAL, Resource.COAL),
                List.of(Resource.OIL, Resource.OIL, Resource.OIL),
                List.of(Resource.GARBAGE, Resource.GARBAGE, Resource.GARBAGE),
                List.of(Resource.URANIUM, Resource.URANIUM, Resource.URANIUM)), pool.requests);
    }

    @Test
    public void stateReportsNormalOrderCurrentPlayerAndCompletion() {
        JSONObject state = new JSONObject(phase.getState());
        assertTrue(state.isNull("currentPlayer"));
        assertTrue(state.getBoolean("finished"));
        startPhase();
        state = new JSONObject(phase.getState());
        assertEquals(List.of("RED", "YELLOW", "BLUE"), state.getJSONArray("playerOrder").toList());
        assertEquals("RED", state.getString("currentPlayer"));
        assertFalse(state.getBoolean("finished"));
        produce(PlayerColor.RED);
        assertEquals("YELLOW", new JSONObject(phase.getState()).getString("currentPlayer"));
        produce(PlayerColor.YELLOW);
        produce(PlayerColor.BLUE);
        state = new JSONObject(phase.getState());
        assertTrue(state.isNull("currentPlayer"));
        assertTrue(state.getBoolean("finished"));
    }

    @Test
    public void nextRoundUsesNewOrderAndInputCollectionsAreCopied() {
        List<PlayerColor> order = new ArrayList<>(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE));
        phase.startPhase(order);
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE), order);
        order.clear();
        Map<PlayerColor, PlayerBoardPort> originalPlayers = new EnumMap<>(players);
        players.clear();
        produce(PlayerColor.RED);
        produce(PlayerColor.YELLOW);
        produce(PlayerColor.BLUE);
        phase.startPhase(List.of(PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.RED));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), produce(PlayerColor.YELLOW));
        assertEquals(Variant.success(PhaseStatus.PHASE_CONTINUES), produce(PlayerColor.BLUE));
        assertEquals(Variant.success(PhaseStatus.PHASE_ENDS), produce(PlayerColor.RED));
        assertEquals(8, pool.requests.size());
        assertEquals(2, powerPlantMarket.prepareCalls);
        for (PlayerBoardPort player : originalPlayers.values()) {
            assertEquals(2, ((BoardFake) player).paidForTowns.size());
        }
    }

    @Test
    public void invalidOrdersAndPlayerCountsAreRejectedWithoutChangingThePhase() {
        startPhase();
        String before = phase.getState();
        for (List<PlayerColor> order : List.of(List.<PlayerColor>of(), List.of(PlayerColor.RED, PlayerColor.BLUE),
                List.of(PlayerColor.RED, PlayerColor.RED, PlayerColor.BLUE),
                List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLACK))) {
            assertThrows(IllegalArgumentException.class, () -> phase.startPhase(order));
            assertEquals(before, phase.getState());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new Bureaucracy(towns, resourceMarket, powerPlantMarket, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Bureaucracy(towns, resourceMarket, powerPlantMarket,
                Map.of(PlayerColor.RED, board(PlayerColor.RED))));
    }

    private final class BoardFake implements PlayerBoardPort {
        private final PlayerColor color;
        private Variant<Integer, RequestError> production = Variant.success(2);
        private List<PowerPlantNumber> selectedPlants = List.of();
        private List<Resource> burnedResources = List.of();
        private final List<Integer> paidForTowns = new ArrayList<>();
        private int productionCalls;

        private BoardFake(final PlayerColor color) {
            this.color = color;
        }

        @Override
        public Variant<Integer, RequestError> usePowerPlants(final List<PowerPlantNumber> numbers,
                final List<Resource> resourcesToBurn) {
            events.add("use:" + color);
            productionCalls++;
            selectedPlants = List.copyOf(numbers);
            burnedResources = List.copyOf(resourcesToBurn);
            return production;
        }

        @Override
        public void receiveMoney(final int numberOfTowns) {
            events.add("pay:" + color);
            paidForTowns.add(numberOfTowns);
        }

        @Override
        public PowerPlantNumber bestPowerPlantNumber() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int outstandingMoney() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void spendMoney(final int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int numberOfConnectableCities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasEnoughMoney(final int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean receivePowerPlant(final PowerPlant plant, final int price) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RequestError> removePowerPlant(final PowerPlantNumber number,
                final List<Resource> removedResources) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RequestError> buyResources(final List<Resource> resources) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String state() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TownsFake implements TownsPort {
        private final Map<PlayerColor, Integer> houses = new EnumMap<>(PlayerColor.class);

        @Override
        public int getNumberOfHouses(final PlayerColor player) {
            return houses.getOrDefault(player, 5);
        }

        @Override
        public Variant<Integer, RequestError> buyHouses(final PlayerColor player,
                final List<String> townNames, final int maxMoney) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getMaxNumberOfHouses() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep2() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep3() {
            throw new UnsupportedOperationException();
        }
    }

    private final class PoolFake implements InterfaceResourcePoolResourceMarket {
        private final List<List<Resource>> requests = new ArrayList<>();

        @Override
        public List<Resource> takeResources(final List<Resource> resources) {
            events.add("refill:" + resources.getFirst());
            requests.add(List.copyOf(resources));
            return List.copyOf(resources);
        }

        @Override
        public boolean canTakeResourcesFromStorageSpace(final List<Resource> resources) {
            return false;
        }

        @Override
        public void getFromStorageSpace(final List<Resource> resources) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getStateString() {
            throw new UnsupportedOperationException();
        }
    }

    private final class MarketFake implements PowerPlantMarketPort {
        private int prepareCalls;

        @Override
        public void prepareNextTurn() {
            events.add("prepare");
            prepareCalls++;
        }

        @Override
        public List<PowerPlant> currentMarket() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PowerPlant> futureMarket() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startAuction() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RequestError> canTakePowerPlant(final PowerPlantNumber number, final int price) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PowerPlant takePowerPlant(final PowerPlantNumber number) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeDiscountedPowerPlant() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removePowerPlantsCheaperThan(final int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep2() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isStep3Start() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep3() {
            throw new UnsupportedOperationException();
        }
    }
}
