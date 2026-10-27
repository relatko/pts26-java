package sk.uniba.fmph.dcs.power_grid;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sk.uniba.fmph.dcs.power_grid.Resource;
import sk.uniba.fmph.dcs.power_grid.InterfaceResourcePoolResourceMarket;

import static org.junit.Assert.*;

final class ResourcePoolFake implements InterfaceResourcePoolResourceMarket {
    List<Resource> pool = new ArrayList<>();
    Resource storageResource = Resource.URANIUM;
    int storageCount = 1000;

    @Override
    public boolean canTakeResourcesFromStorageSpace(List<Resource> resources) {
        List<Resource> res = new ArrayList<>(resources);
        for (Resource r : res) {
            if (r != storageResource) {
                return false;
            }
        }
        return res.size() <= storageCount;
    }

    @Override
    public void getFromStorageSpace(List<Resource> resources) {
        List<Resource> res = new ArrayList<>(resources);
        for (Resource r : res) {
            assert r == storageResource;
        }
        storageCount -= res.size();
        assert storageCount >= 0;
    }

    @Override
    public List<Resource> takeResources(List<Resource> resources) {
        List<Resource> toReturn = new ArrayList<>();
        List<Resource> toTake = new ArrayList<>(resources);
        for (Resource resource : toTake) {
            if (pool.contains(resource)) {
                pool.remove(resource);
                toReturn.add(resource);
            }
        }
        return toReturn;
    }

    @Override
    public String getStateString() {
        return JSONObject.quote("FakePool");
    }
}

public class ResourceMarketTest {

    private ResourcePoolFake pool;
    private ResourceMarket market;

    @Before
    public void setUp() {
        pool = new ResourcePoolFake();
        Map<Integer, Map<Resource, Integer>> refillingPattern = Map.of(1,
                Map.of(Resource.COAL, 0, Resource.OIL, 2, Resource.GARBAGE, 9, Resource.URANIUM, 1), 2,
                Map.of(Resource.COAL, 8, Resource.OIL, 2, Resource.GARBAGE, 0, Resource.URANIUM, 3), 3,
                Map.of(Resource.COAL, 0, Resource.OIL, 2, Resource.GARBAGE, 0, Resource.URANIUM, 8));

        List<Resource> resourcesToMarket = List.of(Resource.COAL, Resource.OIL, Resource.GARBAGE);

        pool.pool.addAll(times(Resource.COAL, ResourceMarket.MAX_RESOURCES.get(Resource.COAL) - 1));
        pool.pool.addAll(times(Resource.OIL, ResourceMarket.MAX_RESOURCES.get(Resource.OIL) - 1));
        pool.pool.addAll(times(Resource.GARBAGE, ResourceMarket.MAX_RESOURCES.get(Resource.GARBAGE) - 1));
        pool.pool.addAll(times(Resource.URANIUM, ResourceMarket.MAX_RESOURCES.get(Resource.URANIUM)));

        market = new ResourceMarket(pool, resourcesToMarket, refillingPattern);
    }

    private static List<Resource> times(Resource resource, int count) {
        List<Resource> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(resource);
        }
        return list;
    }

    private void checkStateString(int expectedCoal, int expectedOil, int expectedGarbage, int expectedUranium) {
        JSONObject state = new JSONObject(market.toString());
        assertEquals(expectedCoal, state.getInt("coal"));
        assertEquals(expectedOil, state.getInt("oil"));
        assertEquals(expectedGarbage, state.getInt("garbage"));
        assertEquals(expectedUranium, state.getInt("uranium"));
    }

    @Test
    public void testRefillWithEnoughResources() {
        checkStateString(1, 1, 1, 0);
        market.refill();
        checkStateString(1, 3, 10, 1);
        market.startStep2();
        market.refill();
        checkStateString(9, 5, 10, 4);
        market.startStep3();
        market.refill();
        checkStateString(9, 7, 10, 12);
    }

    @Test
    public void testRefillWithNotEnoughResources() {
        checkStateString(1, 1, 1, 0);
        market.refill();
        market.refill();
        market.refill();
        checkStateString(1, 7, 24, 3);
        market.startStep2();
        market.startStep3();
        market.refill();
        market.refill();
        checkStateString(1, 11, 24, 12);
    }

    @Test
    public void testPriceCalculationWithoutStorage() {
        market.refill();
        List<Resource> desired = times(Resource.GARBAGE, 7);
        desired.add(Resource.URANIUM);
        assertEquals(Integer.valueOf(1 * 5 + 3 * 6 + 3 * 7 + 16), market.getPrice(desired));
    }

    @Test
    public void testPriceCalculationWithStorage() {
        market.refill();
        market.refill();
        assertEquals(Integer.valueOf(14 + 16 + 2 * 16), market.getPrice(times(Resource.URANIUM, 4)));
    }

    @Test
    public void testPriceCalculationNotEnoughResources() {
        market.refill();
        market.refill();
        assertNull(market.getPrice(times(Resource.COAL, 2)));
    }

    @Test
    public void testTakeResourcesSuccess() {
        market.refill();
        market.takeResources(List.of(Resource.COAL, Resource.GARBAGE, Resource.GARBAGE, Resource.URANIUM));
        checkStateString(0, 3, 8, 0);
        assertEquals(1000, pool.storageCount);
    }

    @Test
    public void testTakeResourcesSuccessUsingStorage() {
        market.refill();
        market.takeResources(List.of(Resource.COAL, Resource.GARBAGE, Resource.URANIUM, Resource.URANIUM));
        checkStateString(0, 3, 9, 0);
        assertEquals(999, pool.storageCount);
    }

    @Test
    public void testTakeResourcesFailure() {
        market.refill();
        assertThrows(AssertionError.class, () -> market.takeResources(List.of(Resource.COAL, Resource.COAL)));
    }
}
