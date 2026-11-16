package sk.uniba.fmph.dcs.power_grid;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlayerPowerPlantsTest {
    private PlayerPowerPlants plants;

    private PowerPlant coalPlant(int number, int resourceAmount, int cities) {
        return new PowerPlant(new PowerPlantNumber(number), List.of(Resource.COAL), resourceAmount, cities);
    }

    private PowerPlant hybridPlant(int number, int resourceAmount, int cities) {
        return new PowerPlant(new PowerPlantNumber(number), List.of(Resource.COAL, Resource.OIL), resourceAmount, cities);
    }

    private PowerPlant greenPlant(int number, int cities) {
        return new PowerPlant(new PowerPlantNumber(number), Collections.emptyList(), 0, cities);
    }

    @Before
    public void setUp() {
        plants = new PlayerPowerPlants();
    }

    @Test
    public void testReceivePowerPlantUpToThree() {
        PowerPlant pp4 = coalPlant(4, 2, 1);
        PowerPlant pp8 = coalPlant(8, 3, 2);
        PowerPlant pp12 = coalPlant(12, 2, 3);
        PowerPlant pp16 = coalPlant(16, 2, 4);

        assertTrue(plants.receivePowerPlant(pp4));
        assertTrue(plants.receivePowerPlant(pp8));
        assertTrue(plants.receivePowerPlant(pp12));
        assertFalse(plants.receivePowerPlant(pp16));
        assertEquals(3, plants.getPowerPlants().size());
    }

    @Test
    public void testBestPowerPlantReturnsHighest() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        plants.receivePowerPlant(coalPlant(12, 2, 3));
        plants.receivePowerPlant(coalPlant(8, 2, 2));
        assertEquals(new PowerPlantNumber(12), plants.bestPowerPlant());
    }

    @Test
    public void testBestPowerPlantEmptyIsNull() {
        assertNull(plants.bestPowerPlant());
    }

    @Test
    public void testRemovePowerPlantSuccess() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        plants.receivePowerPlant(coalPlant(8, 2, 2));

        Optional<RequestError> err = plants.removePowerPlant(new PowerPlantNumber(4), List.of(Resource.COAL, Resource.COAL));
        assertFalse(err.isPresent());
        assertEquals(1, plants.getPowerPlants().size());
        assertEquals(new PowerPlantNumber(8), plants.getPowerPlants().get(0).getNumber());
    }

    @Test
    public void testRemovePowerPlantRejectsExcessRemainingResources() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        plants.receivePowerPlant(coalPlant(8, 1, 2)); // max capacity 2 coal

        Optional<RequestError> err = plants.removePowerPlant(
                new PowerPlantNumber(4),
                List.of(Resource.COAL, Resource.COAL, Resource.COAL)
        );
        assertTrue(err.isPresent());
        assertEquals(2, plants.getPowerPlants().size());
    }

    @Test
    public void testRemovePowerPlantNotFound() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        Optional<RequestError> err = plants.removePowerPlant(new PowerPlantNumber(99), new ArrayList<>());
        assertTrue(err.isPresent());
    }

    @Test
    public void testNumberOfConnectableCitiesSufficientResources() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        plants.receivePowerPlant(coalPlant(8, 3, 2));

        List<Resource> resources = Arrays.asList(
                Resource.COAL, Resource.COAL,
                Resource.COAL, Resource.COAL, Resource.COAL
        );
        assertEquals(3, plants.numberOfConnectableCities(resources));
    }

    @Test
    public void testNumberOfConnectableCitiesChoosesOptimalSubset() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        plants.receivePowerPlant(coalPlant(10, 2, 3));

        // Only 2 coal available: optimal choice powers plant 10 (3 cities) rather than plant 4 (1 city)
        assertEquals(3, plants.numberOfConnectableCities(List.of(Resource.COAL, Resource.COAL)));
    }

    @Test
    public void testNumberOfConnectableCitiesInsufficientResources() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        assertEquals(0, plants.numberOfConnectableCities(List.of(Resource.COAL)));
    }

    @Test
    public void testCanStoreResourcesFits() {
        plants.receivePowerPlant(coalPlant(10, 2, 3));
        List<Resource> toStore = Arrays.asList(Resource.COAL, Resource.COAL, Resource.COAL);
        assertTrue(plants.canStoreResources(toStore));
    }

    @Test
    public void testCanStoreResourcesHybridAndPureOptimalAllocation() {
        // Hybrid plant listed first (capacity 4 coal/oil), pure coal plant second (capacity 4 coal)
        plants.receivePowerPlant(hybridPlant(5, 2, 2));
        plants.receivePowerPlant(coalPlant(10, 2, 3));

        List<Resource> toStore = Arrays.asList(
                Resource.COAL, Resource.COAL, Resource.COAL, Resource.COAL,
                Resource.OIL, Resource.OIL, Resource.OIL, Resource.OIL
        );
        assertTrue(plants.canStoreResources(toStore));
    }

    @Test
    public void testCanStoreResourcesOverflow() {
        plants.receivePowerPlant(coalPlant(5, 1, 2));
        List<Resource> toStore = Arrays.asList(Resource.COAL, Resource.COAL, Resource.COAL);
        assertFalse(plants.canStoreResources(toStore));
    }

    @Test
    public void testCanStoreResourcesWrongType() {
        plants.receivePowerPlant(coalPlant(5, 2, 2));
        assertFalse(plants.canStoreResources(List.of(Resource.OIL)));
    }

    @Test
    public void testUsePowerPlantSuccess() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        Object result = plants.usePowerPlant(
                List.of(new PowerPlantNumber(4)),
                Arrays.asList(Resource.COAL, Resource.COAL)
        );
        assertTrue(result instanceof Integer);
        assertEquals(1, (int) (Integer) result);
    }

    @Test
    public void testUsePowerPlantNotFound() {
        Object result = plants.usePowerPlant(List.of(new PowerPlantNumber(99)), new ArrayList<>());
        assertTrue(result instanceof RequestError);
    }

    @Test
    public void testUsePowerPlantDuplicatePlantRejected() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        Object result = plants.usePowerPlant(
                List.of(new PowerPlantNumber(4), new PowerPlantNumber(4)),
                List.of(Resource.COAL, Resource.COAL, Resource.COAL, Resource.COAL)
        );
        assertTrue(result instanceof RequestError);
    }

    @Test
    public void testUsePowerPlantNotEnoughResources() {
        plants.receivePowerPlant(coalPlant(4, 2, 1));
        Object result = plants.usePowerPlant(List.of(new PowerPlantNumber(4)), List.of(Resource.COAL));
        assertTrue(result instanceof RequestError);
    }

    @Test
    public void testHybridAndPurePlantSimultaneousUse() {
        plants.receivePowerPlant(hybridPlant(5, 2, 2));
        plants.receivePowerPlant(coalPlant(10, 2, 3));

        Object result = plants.usePowerPlant(
                List.of(new PowerPlantNumber(5), new PowerPlantNumber(10)),
                Arrays.asList(Resource.COAL, Resource.COAL, Resource.OIL, Resource.OIL)
        );
        assertTrue(result instanceof Integer);
        assertEquals(5, (int) (Integer) result);
    }

    @Test
    public void testGreenPlantRequiresNoResources() {
        plants.receivePowerPlant(greenPlant(13, 1));
        assertEquals(1, plants.numberOfConnectableCities(Collections.emptyList()));
        Object result = plants.usePowerPlant(List.of(new PowerPlantNumber(13)), Collections.emptyList());
        assertEquals(1, (int) (Integer) result);
    }
}
