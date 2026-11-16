package sk.uniba.fmph.dcs.power_grid;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ResourcePoolTest {
    private ResourcePool pool;

    private static List<Resource> times(Resource r, int n) {
        List<Resource> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(r);
        }
        return list;
    }

    @Before
    public void setUp() {
        List<Resource> initial = new ArrayList<>();
        initial.addAll(times(Resource.COAL, 10));
        initial.addAll(times(Resource.OIL, 5));
        initial.addAll(times(Resource.GARBAGE, 3));
        initial.addAll(times(Resource.URANIUM, 2));
        pool = new ResourcePool(initial);
    }

    @Test
    public void testCanTakeResourcesFromStorageSpaceSufficient() {
        List<Resource> req = times(Resource.COAL, 5);
        assertTrue(pool.canTakeResourcesFromStorageSpace(req));
    }

    @Test
    public void testCanTakeResourcesFromStorageSpaceInsufficient() {
        List<Resource> req = times(Resource.COAL, 11);
        assertFalse(pool.canTakeResourcesFromStorageSpace(req));
    }

    @Test
    public void testGetFromStorageSpaceReducesStock() {
        List<Resource> req = times(Resource.OIL, 3);
        pool.getFromStorageSpace(req);
        assertFalse(pool.canTakeResourcesFromStorageSpace(times(Resource.OIL, 3)));
        assertTrue(pool.canTakeResourcesFromStorageSpace(times(Resource.OIL, 2)));
    }

    @Test
    public void testGetFromStorageSpaceThrowsAssertionWhenInsufficient() {
        assertThrows(AssertionError.class, () -> pool.getFromStorageSpace(times(Resource.URANIUM, 5)));
    }

    @Test
    public void testTakeResourcesFullAmount() {
        List<Resource> req = times(Resource.COAL, 5);
        List<Resource> taken = pool.takeResources(req);
        assertEquals(5, taken.size());
        assertTrue(taken.stream().allMatch(r -> r == Resource.COAL));
    }

    @Test
    public void testTakeResourcesPartialAmount() {
        List<Resource> req = times(Resource.GARBAGE, 10);
        List<Resource> taken = pool.takeResources(req);
        assertEquals(3, taken.size());
    }

    @Test
    public void testPutResourcesViaPlayerResourcesInterface() {
        InterfaceResourcePoolPlayerResources playerPool = pool;
        pool.takeResources(times(Resource.COAL, 10));
        assertFalse(pool.canTakeResourcesFromStorageSpace(times(Resource.COAL, 1)));

        playerPool.putResources(times(Resource.COAL, 4));
        assertTrue(pool.canTakeResourcesFromStorageSpace(times(Resource.COAL, 4)));
    }

    @Test
    public void testGetStateStringFormat() {
        String state = pool.getStateString();
        assertTrue(state.contains("\"coal\":10"));
        assertTrue(state.contains("\"oil\":5"));
        assertTrue(state.contains("\"garbage\":3"));
        assertTrue(state.contains("\"uranium\":2"));
        assertEquals(state, pool.toString());
    }

    @Test
    public void testEmptyPool() {
        ResourcePool empty = new ResourcePool(new ArrayList<>());
        assertFalse(empty.canTakeResourcesFromStorageSpace(times(Resource.COAL, 1)));
        List<Resource> taken = empty.takeResources(times(Resource.COAL, 5));
        assertEquals(0, taken.size());
    }
}
