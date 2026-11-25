package sk.uniba.fmph.dcs.power_grid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StoreResourcesTest {

    private static final PowerPlant COAL_2 = plant(4, 2, Resource.COAL);
    private static final PowerPlant OIL_2 = plant(3, 2, Resource.OIL);
    private static final PowerPlant GARBAGE_1 = plant(6, 1, Resource.GARBAGE);
    private static final PowerPlant URANIUM_1 = plant(11, 1, Resource.URANIUM);
    private static final PowerPlant HYBRID_2 = plant(5, 2, Resource.COAL, Resource.OIL);
    private static final PowerPlant ECO = plant(13, 0);

    private final StoreResources storeResources = new StoreResources();

    private static PowerPlant plant(final int number, final int amount, final Resource... accepted) {
        return new PowerPlant(new PowerPlantNumber(number), List.of(accepted), amount, 1);
    }

    private static List<Resource> resources(final int coal, final int oil, final int garbage, final int uranium) {
        List<Resource> result = new ArrayList<>();
        result.addAll(Collections.nCopies(uranium, Resource.URANIUM));
        result.addAll(Collections.nCopies(oil, Resource.OIL));
        result.addAll(Collections.nCopies(garbage, Resource.GARBAGE));
        result.addAll(Collections.nCopies(coal, Resource.COAL));
        return result;
    }

    private void assertFits(final List<Resource> resources, final PowerPlant... plants) {
        assertTrue(storeResources.canStoreResources(resources, List.of(plants)));
    }

    private void assertDoesNotFit(final List<Resource> resources, final PowerPlant... plants) {
        assertFalse(storeResources.canStoreResources(resources, List.of(plants)));
    }

    @Test
    public void noResourcesAlwaysFit() {
        assertFits(List.of());
        assertFits(List.of(), ECO);
        assertFits(List.of(), COAL_2);
    }

    @Test
    public void nothingFitsWithoutPlants() {
        assertDoesNotFit(resources(1, 0, 0, 0));
    }

    @Test
    public void ecologicalPlantStoresNothing() {
        for (Resource resource : Resource.values()) {
            assertDoesNotFit(List.of(resource), ECO);
        }
        assertDoesNotFit(resources(5, 0, 0, 0), COAL_2, ECO);
    }

    @Test
    public void plantStoresExactlyTwiceItsConsumption() {
        assertFits(resources(4, 0, 0, 0), COAL_2);
        assertDoesNotFit(resources(5, 0, 0, 0), COAL_2);
        assertFits(resources(0, 0, 2, 0), GARBAGE_1);
        assertDoesNotFit(resources(0, 0, 3, 0), GARBAGE_1);
        assertFits(resources(0, 0, 0, 2), URANIUM_1);
        assertDoesNotFit(resources(0, 0, 0, 3), URANIUM_1);
    }

    @Test
    public void plantRejectsOtherTypes() {
        assertDoesNotFit(resources(0, 1, 0, 0), COAL_2);
        assertDoesNotFit(resources(0, 0, 1, 0), HYBRID_2);
        assertDoesNotFit(resources(0, 0, 0, 1), HYBRID_2);
    }

    @Test
    public void capacitiesOfSameTypePlantsAddUp() {
        assertFits(resources(8, 0, 0, 0), COAL_2, plant(8, 2, Resource.COAL));
        assertDoesNotFit(resources(9, 0, 0, 0), COAL_2, plant(8, 2, Resource.COAL));
    }

    @Test
    public void hybridStoresAnyMixUpToItsCapacity() {
        for (int coal = 0; coal <= 4; coal++) {
            assertFits(resources(coal, 4 - coal, 0, 0), HYBRID_2);
            assertDoesNotFit(resources(coal, 5 - coal, 0, 0), HYBRID_2);
        }
        assertDoesNotFit(resources(5, 0, 0, 0), HYBRID_2);
    }

    @Test
    public void hybridTakesOverflowOfBothTypes() {
        assertFits(resources(6, 6, 0, 0), HYBRID_2, COAL_2, OIL_2);
        assertFits(resources(8, 4, 0, 0), COAL_2, HYBRID_2, OIL_2);
        assertDoesNotFit(resources(7, 6, 0, 0), COAL_2, OIL_2, HYBRID_2);
        assertDoesNotFit(resources(9, 0, 0, 0), COAL_2, OIL_2, HYBRID_2);
    }

    @Test
    public void mixedPlantsFillCompletely() {
        assertFits(resources(2, 2, 2, 2), HYBRID_2, GARBAGE_1, URANIUM_1);
        assertDoesNotFit(resources(2, 2, 3, 1), HYBRID_2, GARBAGE_1, URANIUM_1);
        assertDoesNotFit(resources(2, 2, 1, 3), HYBRID_2, GARBAGE_1, URANIUM_1);
    }

    @Test
    public void overlappingHybridsAreHandled() {
        PowerPlant coalOil = plant(20, 1, Resource.COAL, Resource.OIL);
        PowerPlant oilGarbage = plant(21, 1, Resource.OIL, Resource.GARBAGE);
        assertFits(resources(2, 0, 2, 0), coalOil, oilGarbage);
        assertFits(resources(1, 2, 1, 0), coalOil, oilGarbage);
        assertDoesNotFit(resources(3, 0, 1, 0), coalOil, oilGarbage);
        assertDoesNotFit(resources(1, 3, 1, 0), coalOil, oilGarbage);
    }

    @Test
    public void duplicateAcceptedTypesDoNotIncreaseCapacity() {
        PowerPlant duplicated = plant(22, 1, Resource.COAL, Resource.COAL);
        assertFits(resources(2, 0, 0, 0), duplicated);
        assertDoesNotFit(resources(3, 0, 0, 0), duplicated);
    }

    @Test
    public void inputsAreNotModified() {
        List<Resource> resources = resources(2, 1, 0, 0);
        List<Resource> original = List.copyOf(resources);
        List<PowerPlant> plants = new ArrayList<>(List.of(COAL_2, HYBRID_2));
        storeResources.canStoreResources(resources, plants);
        assertEquals(original, resources);
        assertEquals(List.of(COAL_2, HYBRID_2), plants);
    }
}
