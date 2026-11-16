package sk.uniba.fmph.dcs.power_grid;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BuildingHousesTest {

    static class FakeFacade implements InterfacePlayerBoardFacade {
        int money;
        int cities;

        FakeFacade(int money, int cities) {
            this.money = money;
            this.cities = cities;
        }

        @Override
        public boolean hasEnoughMoney(int amount) {
            return money >= amount;
        }

        @Override
        public void spendMoney(int amount) {
            money -= amount;
        }

        @Override
        public int numberOfConnectedCities() {
            return cities;
        }
    }

    static class FakeTowns implements InterfaceTownsBuildingHouses {
        @Override
        public Object buyHouses(PlayerColor player, List<String> townNames) {
            return townNames.size() * 10;
        }
    }

    static class ErrorTowns implements InterfaceTownsBuildingHouses {
        @Override
        public Object buyHouses(PlayerColor player, List<String> townNames) {
            return new RequestError("City not available.");
        }
    }

    private Map<PlayerColor, InterfacePlayerBoardFacade> facades;
    private BuildingHouses building;

    @Before
    public void setUp() {
        facades = new HashMap<>();
        facades.put(PlayerColor.RED, new FakeFacade(100, 5));
        facades.put(PlayerColor.YELLOW, new FakeFacade(100, 3));
        facades.put(PlayerColor.BLUE, new FakeFacade(100, 1));
        building = new BuildingHouses(facades, new FakeTowns());
    }

    @Test
    public void testPhaseEndsAfterAllPlayersTurn() {
        List<PlayerColor> order = Arrays.asList(PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        building.startPhase(order);

        Object r1 = building.buyHouses(PlayerColor.RED, List.of("CityA", "CityB"));
        assertEquals(PhaseStatus.PHASE_CONTINUES, r1);

        Object r2 = building.buyHouses(PlayerColor.YELLOW, List.of("CityC"));
        assertEquals(PhaseStatus.PHASE_CONTINUES, r2);

        Object r3 = building.buyHouses(PlayerColor.BLUE, List.of());
        assertEquals(PhaseStatus.PHASE_ENDS, r3);
    }

    @Test
    public void testGameEndsWhenThresholdReachedForThreePlayers() {
        facades.put(PlayerColor.RED, new FakeFacade(100, 17));
        building = new BuildingHouses(facades, new FakeTowns());

        List<PlayerColor> order = Arrays.asList(PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        building.startPhase(order);

        assertEquals(PhaseStatus.PHASE_CONTINUES, building.buyHouses(PlayerColor.RED, List.of()));
        assertEquals(PhaseStatus.PHASE_CONTINUES, building.buyHouses(PlayerColor.YELLOW, List.of()));
        assertEquals(PhaseStatus.GAME_ENDS, building.buyHouses(PlayerColor.BLUE, List.of()));
    }

    @Test
    public void testWrongPlayerTurnReturnsError() {
        List<PlayerColor> order = Arrays.asList(PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        building.startPhase(order);

        Object result = building.buyHouses(PlayerColor.YELLOW, List.of("CityA"));
        assertTrue(result instanceof RequestError);
    }

    @Test
    public void testMoneyDeducted() {
        List<PlayerColor> order = Arrays.asList(PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        building.startPhase(order);

        FakeFacade redFacade = (FakeFacade) facades.get(PlayerColor.RED);
        int before = redFacade.money;

        building.buyHouses(PlayerColor.RED, List.of("CityA", "CityB"));
        assertEquals(before - 20, redFacade.money);
    }

    @Test
    public void testNotEnoughMoneyReturnsErrorAndAllowsRetry() {
        facades.put(PlayerColor.RED, new FakeFacade(15, 5));
        building = new BuildingHouses(facades, new FakeTowns());

        List<PlayerColor> order = Arrays.asList(PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        building.startPhase(order);

        // 2 cities costs 20, RED has 15 -> error
        Object result = building.buyHouses(PlayerColor.RED, List.of("CityA", "CityB"));
        assertTrue(result instanceof RequestError);

        // Retry with 1 city (cost 10) succeeds
        Object retry = building.buyHouses(PlayerColor.RED, List.of("CityA"));
        assertEquals(PhaseStatus.PHASE_CONTINUES, retry);
    }

    @Test
    public void testTownErrorPropagated() {
        building = new BuildingHouses(facades, new ErrorTowns());
        List<PlayerColor> order = List.of(PlayerColor.RED);
        building.startPhase(order);

        Object result = building.buyHouses(PlayerColor.RED, List.of("CityX"));
        assertTrue(result instanceof RequestError);
    }

    @Test
    public void testPhaseNotActive() {
        Object result = building.buyHouses(PlayerColor.RED, List.of("CityA"));
        assertTrue(result instanceof RequestError);
    }

    @Test
    public void testSinglePlayerPhase() {
        List<PlayerColor> order = List.of(PlayerColor.RED);
        building.startPhase(order);

        Object result = building.buyHouses(PlayerColor.RED, new ArrayList<>());
        assertEquals(PhaseStatus.PHASE_ENDS, result);
    }

    @Test
    public void testGetStateContainsPhaseActive() {
        List<PlayerColor> order = List.of(PlayerColor.RED);
        building.startPhase(order);

        String state = building.getState();
        assertTrue(state.contains("phaseActive"));
    }
}
