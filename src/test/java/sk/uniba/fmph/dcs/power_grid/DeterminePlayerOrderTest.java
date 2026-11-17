package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class DeterminePlayerOrderTest {

    private FakeTowns towns;
    private Map<PlayerColor, InterfacePlayerBoardDeterminePlayerOrder> boards;
    private FakePlayerBoard redBoard;
    private FakePlayerBoard yellowBoard;
    private FakePlayerBoard blueBoard;
    private FakePlayerBoard greenBoard;

    private static final class FakeTowns implements InterfaceTownsDeterminePlayerOrder {
        private final Map<PlayerColor, Integer> houses = new HashMap<>();

        void setHouses(final PlayerColor player, final int count) {
            houses.put(player, count);
        }

        @Override
        public int getNumberOfHouses(final PlayerColor player) {
            return houses.getOrDefault(player, 0);
        }
    }

    private static final class FakePlayerBoard implements InterfacePlayerBoardDeterminePlayerOrder {
        private PowerPlantNumber bestPlant;
        private int connectableCities;
        private int money;

        FakePlayerBoard(final PowerPlantNumber bestPlant, final int connectableCities, final int money) {
            this.bestPlant = bestPlant;
            this.connectableCities = connectableCities;
            this.money = money;
        }

        void setBestPlant(final PowerPlantNumber bestPlant) {
            this.bestPlant = bestPlant;
        }

        void setConnectableCities(final int connectableCities) {
            this.connectableCities = connectableCities;
        }

        void setMoney(final int money) {
            this.money = money;
        }

        @Override
        public PowerPlantNumber bestPowerPlantNumber() {
            return bestPlant;
        }

        @Override
        public int numberOfConnectableCities() {
            return connectableCities;
        }

        @Override
        public int outstandingMoney() {
            return money;
        }
    }

    @Before
    public void setUp() {
        towns = new FakeTowns();
        boards = new HashMap<>();

        redBoard = new FakePlayerBoard(new PowerPlantNumber(10), 3, 50);
        yellowBoard = new FakePlayerBoard(new PowerPlantNumber(15), 4, 40);
        blueBoard = new FakePlayerBoard(new PowerPlantNumber(20), 5, 30);
        greenBoard = new FakePlayerBoard(new PowerPlantNumber(25), 6, 20);

        boards.put(PlayerColor.RED, redBoard);
        boards.put(PlayerColor.YELLOW, yellowBoard);
        boards.put(PlayerColor.BLUE, blueBoard);
        boards.put(PlayerColor.GREEN, greenBoard);
    }

    @Test
    public void testReorderByCityCount() {
        towns.setHouses(PlayerColor.RED, 3);
        towns.setHouses(PlayerColor.YELLOW, 5);
        towns.setHouses(PlayerColor.BLUE, 2);
        towns.setHouses(PlayerColor.GREEN, 4);

        List<PlayerColor> initialOrder = List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.GREEN);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        List<PlayerColor> newOrder = dpo.reorder();
        assertEquals(List.of(PlayerColor.YELLOW, PlayerColor.GREEN, PlayerColor.RED, PlayerColor.BLUE), newOrder);
    }

    @Test
    public void testReorderRulebookExample() {
        // Official rulebook example:
        // Anna (RED) has 6 cities.
        // Greg (YELLOW) and Al (BLUE) have 5 cities each. Greg's largest is 17, Al's largest is 15.
        // Natalie (GREEN) has 4 cities.
        // Expected order: RED, YELLOW, BLUE, GREEN.
        towns.setHouses(PlayerColor.RED, 6);
        towns.setHouses(PlayerColor.YELLOW, 5);
        towns.setHouses(PlayerColor.BLUE, 5);
        towns.setHouses(PlayerColor.GREEN, 4);

        redBoard.setBestPlant(new PowerPlantNumber(10));
        yellowBoard.setBestPlant(new PowerPlantNumber(17));
        blueBoard.setBestPlant(new PowerPlantNumber(15));
        greenBoard.setBestPlant(new PowerPlantNumber(30));

        List<PlayerColor> initialOrder = List.of(PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        List<PlayerColor> newOrder = dpo.reorder();
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.GREEN), newOrder);
    }

    @Test
    public void testReorderPreservesRelativeOrderOnFullTie() {
        // All players have 0 cities and no power plants (start of game).
        redBoard.setBestPlant(null);
        yellowBoard.setBestPlant(null);
        blueBoard.setBestPlant(null);
        greenBoard.setBestPlant(null);

        List<PlayerColor> initialOrder = List.of(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        List<PlayerColor> newOrder = dpo.reorder();
        assertEquals(initialOrder, newOrder);
    }

    @Test
    public void testReorderConsecutiveCallsPreserveUpdatedOrder() {
        towns.setHouses(PlayerColor.RED, 2);
        towns.setHouses(PlayerColor.YELLOW, 2);
        redBoard.setBestPlant(new PowerPlantNumber(20));
        yellowBoard.setBestPlant(new PowerPlantNumber(10));

        List<PlayerColor> initialOrder = List.of(PlayerColor.YELLOW, PlayerColor.RED);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        // First reorder: RED has bigger plant, so RED becomes 1st, YELLOW 2nd
        List<PlayerColor> order1 = dpo.reorder();
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW), order1);

        // Now both buy equal plants or tie on plants
        yellowBoard.setBestPlant(new PowerPlantNumber(20));
        List<PlayerColor> order2 = dpo.reorder();
        // Since RED was now ahead in currentOrder, RED stays first
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW), order2);
    }

    @Test
    public void testGameResultPrimaryCriterionSuppliedCities() {
        // Player GREEN connected 17 cities, but can only supply 10 cities.
        // Player BLUE connected 14 cities, and can supply 14 cities.
        // Player RED connected 12 cities, and can supply 12 cities.
        // Winner must be BLUE (14 supplied), then RED (12 supplied), then GREEN (10 supplied).
        towns.setHouses(PlayerColor.GREEN, 17);
        greenBoard.setConnectableCities(10);

        towns.setHouses(PlayerColor.BLUE, 14);
        blueBoard.setConnectableCities(14);

        towns.setHouses(PlayerColor.RED, 12);
        redBoard.setConnectableCities(15); // capped by 12 houses

        List<PlayerColor> initialOrder = List.of(PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.RED);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        List<PlayerColor> result = dpo.gameResult();
        assertEquals(List.of(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.GREEN), result);
    }

    @Test
    public void testGameResultTieBrokenByRemainingMoney() {
        // Both supply 15 cities.
        // RED has 70 Elektro, YELLOW has 45 Elektro.
        towns.setHouses(PlayerColor.RED, 15);
        redBoard.setConnectableCities(15);
        redBoard.setMoney(70);

        towns.setHouses(PlayerColor.YELLOW, 15);
        yellowBoard.setConnectableCities(15);
        yellowBoard.setMoney(45);

        List<PlayerColor> initialOrder = List.of(PlayerColor.YELLOW, PlayerColor.RED);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        List<PlayerColor> result = dpo.gameResult();
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW), result);
    }

    @Test
    public void testGameResultTieBrokenByConnectedCitiesThenPlant() {
        // Both supply 10 cities and both have 50 Elektro.
        // RED has 15 connected cities (wasted 5).
        // YELLOW has 10 connected cities.
        towns.setHouses(PlayerColor.RED, 15);
        redBoard.setConnectableCities(10);
        redBoard.setMoney(50);

        towns.setHouses(PlayerColor.YELLOW, 10);
        yellowBoard.setConnectableCities(10);
        yellowBoard.setMoney(50);

        List<PlayerColor> initialOrder = List.of(PlayerColor.YELLOW, PlayerColor.RED);
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(initialOrder, towns, boards);

        List<PlayerColor> result = dpo.gameResult();
        assertEquals(List.of(PlayerColor.RED, PlayerColor.YELLOW), result);

        // If also connected cities tie, tie broken by highest power plant
        towns.setHouses(PlayerColor.RED, 10);
        redBoard.setBestPlant(new PowerPlantNumber(25));
        yellowBoard.setBestPlant(new PowerPlantNumber(30));

        List<PlayerColor> result2 = dpo.gameResult();
        assertEquals(List.of(PlayerColor.YELLOW, PlayerColor.RED), result2);
    }

    @Test
    public void testImmutabilityAndDefensiveCopy() {
        List<PlayerColor> mutableList = new ArrayList<>(List.of(PlayerColor.RED, PlayerColor.BLUE));
        DeterminePlayerOrder dpo = new DeterminePlayerOrder(mutableList, towns, boards);

        mutableList.clear(); // mutating external list should not affect dpo
        List<PlayerColor> order = dpo.reorder();
        assertEquals(2, order.size());

        try {
            order.remove(0);
            fail("Returned list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // Success
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullOrder() {
        new DeterminePlayerOrder(null, towns, boards);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullTowns() {
        new DeterminePlayerOrder(List.of(PlayerColor.RED), null, boards);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullBoards() {
        new DeterminePlayerOrder(List.of(PlayerColor.RED), towns, null);
    }
}
