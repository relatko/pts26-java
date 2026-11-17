package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class PlayerBoardFacadeTest {

    private FakePlayerMoney money;
    private FakePlayerPowerPlants powerPlants;
    private FakePlayerResources resources;
    private FakeTownsPlayerBoard towns;
    private PlayerBoardFacade facade;

    private static final class FakePlayerMoney implements InterfacePlayerMoney {
        private int currentCash;

        FakePlayerMoney(final int initialCash) {
            this.currentCash = initialCash;
        }

        @Override
        public boolean hasEnoughMoney(final int amount) {
            return currentCash >= amount;
        }

        @Override
        public void spendMoney(final int amount) {
            currentCash -= amount;
        }

        @Override
        public void receiveMoney(final int numberOfTowns) {
            currentCash += numberOfTowns * 10;
        }

        @Override
        public int currentMoney() {
            return currentCash;
        }
    }

    private static final class FakePlayerPowerPlants implements InterfacePlayerPowerPlants {
        private final List<PowerPlant> ownedPlants = new ArrayList<>();
        private boolean limitExceeded = false;

        @Override
        public PowerPlantNumber bestPowerPlant() {
            if (ownedPlants.isEmpty()) {
                return null;
            }
            PowerPlant best = ownedPlants.get(0);
            for (PowerPlant pp : ownedPlants) {
                if (pp.getNumber().getValue() > best.getNumber().getValue()) {
                    best = pp;
                }
            }
            return best.getNumber();
        }

        @Override
        public boolean receivePowerPlant(final PowerPlant powerPlant) {
            ownedPlants.add(powerPlant);
            return !limitExceeded && ownedPlants.size() <= 3;
        }

        @Override
        public boolean receivePowerPlant(final PowerPlantNumber powerPlantNumber) {
            PowerPlant plant = new PowerPlant(powerPlantNumber, Collections.emptyList(), 0, 1);
            return receivePowerPlant(plant);
        }

        @Override
        public Optional<RequestError> removePowerPlant(
                final PowerPlantNumber powerPlantNumber,
                final List<Resource> remainingResources) {
            PowerPlant toRemove = null;
            for (PowerPlant pp : ownedPlants) {
                if (pp.getNumber().equals(powerPlantNumber)) {
                    toRemove = pp;
                    break;
                }
            }
            if (toRemove == null) {
                return Optional.of(new RequestError("Power plant not owned: " + powerPlantNumber));
            }
            ownedPlants.remove(toRemove);
            return Optional.empty();
        }

        @Override
        public int numberOfConnectableCities(final List<Resource> resources) {
            return ownedPlants.size() * 2;
        }

        @Override
        public boolean canStoreResources(final List<Resource> resources) {
            return true;
        }

        @Override
        public List<PowerPlant> getPowerPlants() {
            return Collections.unmodifiableList(ownedPlants);
        }
    }

    private static final class FakePlayerResources implements InterfacePlayerResources {
        private final List<Resource> stored = new ArrayList<>();
        private int connectableCapacity = 5;

        @Override
        public Optional<RequestError> buyResources(final List<Resource> resourcesToBuy) {
            stored.addAll(resourcesToBuy);
            return Optional.empty();
        }

        @Override
        public Optional<RequestError> usePowerPlants(
                final PlayerColor playerColor,
                final List<PowerPlantNumber> powerPlantNumbers,
                final List<Resource> resourcesToBurn) {
            if (powerPlantNumbers.isEmpty()) {
                return Optional.of(new RequestError("No power plants selected to burn"));
            }
            return Optional.empty();
        }

        @Override
        public int numberOfConnectableCities() {
            return connectableCapacity;
        }

        @Override
        public Optional<RequestError> removePowerPlant(
                final PowerPlantNumber powerPlantNumber,
                final List<Resource> remainingResources) {
            return Optional.empty();
        }

        @Override
        public List<Resource> getResources() {
            return Collections.unmodifiableList(stored);
        }
    }

    private static final class FakeTownsPlayerBoard implements InterfaceTownsPlayerBoard {
        private int houses = 3;

        @Override
        public int getNumberOfHouses(final PlayerColor player) {
            return houses;
        }
    }

    @Before
    public void setUp() {
        money = new FakePlayerMoney(50);
        powerPlants = new FakePlayerPowerPlants();
        resources = new FakePlayerResources();
        towns = new FakeTownsPlayerBoard();

        facade = new PlayerBoardFacade(PlayerColor.RED, money, powerPlants, resources, towns);
    }

    @Test
    public void testBasicPropertiesAndDelegations() {
        assertEquals(PlayerColor.RED, facade.getPlayerColor());
        assertEquals(50, facade.outstandingMoney());
        assertTrue(facade.hasEnoughMoney(30));
        assertFalse(facade.hasEnoughMoney(60));

        facade.spendMoney(20);
        assertEquals(30, facade.outstandingMoney());

        assertEquals(5, facade.numberOfConnectableCities());
        assertEquals(3, facade.numberOfConnectedCities());
    }

    @Test
    public void testReceivePowerPlantWithinLimit() {
        PowerPlantNumber ppn = new PowerPlantNumber(10);
        boolean withinLimit = facade.receivePowerPlant(ppn, 15);

        assertTrue(withinLimit);
        assertEquals(35, facade.outstandingMoney());
        assertEquals(ppn, facade.bestPowerPlantNumber());
    }

    @Test
    public void testReceivePowerPlantInsufficientFunds() {
        PowerPlantNumber ppn = new PowerPlantNumber(15);
        boolean received = facade.receivePowerPlant(ppn, 100);

        assertFalse(received);
        assertEquals(50, facade.outstandingMoney());
    }

    @Test
    public void testReceivePowerPlantExceedsLimit() {
        facade.receivePowerPlant(new PowerPlantNumber(3), 5);
        facade.receivePowerPlant(new PowerPlantNumber(4), 5);
        facade.receivePowerPlant(new PowerPlantNumber(5), 5);

        // Buying 4th power plant: allowed to buy, but returns false because above limit of 3
        boolean withinLimit = facade.receivePowerPlant(new PowerPlantNumber(6), 5);
        assertFalse(withinLimit);
        assertEquals(30, facade.outstandingMoney());
    }

    @Test
    public void testRemovePowerPlant() {
        PowerPlantNumber ppn = new PowerPlantNumber(13);
        facade.receivePowerPlant(ppn, 10);

        Optional<RequestError> error = facade.removePowerPlant(ppn, List.of(Resource.COAL));
        assertTrue(error.isEmpty());

        // Removing non-existent plant returns RequestError
        Optional<RequestError> error2 = facade.removePowerPlant(new PowerPlantNumber(99), Collections.emptyList());
        assertTrue(error2.isPresent());
    }

    @Test
    public void testBuyResources() {
        Optional<RequestError> error = facade.buyResources(List.of(Resource.COAL, Resource.OIL));
        assertTrue(error.isEmpty());
        assertEquals(2, resources.getResources().size());
    }

    @Test
    public void testUsePowerPlantsSuccessAndMismatch() {
        Optional<RequestError> success = facade.usePowerPlants(
                PlayerColor.RED,
                List.of(new PowerPlantNumber(10)),
                List.of(Resource.COAL));
        assertTrue(success.isEmpty());

        // Mismatched player color returns error
        Optional<RequestError> mismatch = facade.usePowerPlants(
                PlayerColor.BLUE,
                List.of(new PowerPlantNumber(10)),
                List.of(Resource.COAL));
        assertTrue(mismatch.isPresent());
        assertTrue(mismatch.get().getDescription().contains("Player color mismatch"));
    }

    @Test
    public void testStateSerialization() {
        facade.receivePowerPlant(new PowerPlantNumber(15), 10);
        String state = facade.state();

        assertTrue(state.contains("\"player\":\"RED\""));
        assertTrue(state.contains("\"money\":40"));
        assertTrue(state.contains("\"bestPowerPlant\":15"));
        assertTrue(state.contains("\"connectableCities\":5"));
        assertTrue(state.contains("\"connectedCities\":3"));
    }

    @Test
    public void testConstructorWithoutTownsQuery() {
        PlayerBoardFacade facadeNoTowns = new PlayerBoardFacade(PlayerColor.BLUE, money, powerPlants, resources);
        assertEquals(0, facadeNoTowns.numberOfConnectedCities());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullPlayerColor() {
        new PlayerBoardFacade(null, money, powerPlants, resources);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMoney() {
        new PlayerBoardFacade(PlayerColor.RED, null, powerPlants, resources);
    }
}
