package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ResourcePool represents the supply of resource tokens outside the resource
 * market.
 * Total tokens in the game: COAL=24, OIL=24, GARBAGE=24, URANIUM=12.
 */
public final class ResourcePool implements InterfaceResourcePoolResourceMarket, InterfaceResourcePoolPlayerResources {
    private final Map<Resource, Integer> stock = new EnumMap<>(Resource.class);

    public ResourcePool(final List<Resource> initialResources) {
        Objects.requireNonNull(initialResources, "initialResources must not be null");
        for (Resource r : Resource.values()) {
            stock.put(r, 0);
        }
        for (Resource r : initialResources) {
            Objects.requireNonNull(r, "resource element must not be null");
            stock.merge(r, 1, Integer::sum);
        }
        for (Resource r : Resource.values()) {
            assert stock.get(r) <= ResourceMarket.MAX_RESOURCES.get(r);
        }
    }

    @Override
    public boolean canTakeResourcesFromStorageSpace(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        Map<Resource, Integer> needed = count(resources);
        for (Map.Entry<Resource, Integer> entry : needed.entrySet()) {
            if (stock.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void getFromStorageSpace(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        assert canTakeResourcesFromStorageSpace(resources);
        Map<Resource, Integer> needed = count(resources);
        for (Map.Entry<Resource, Integer> entry : needed.entrySet()) {
            stock.merge(entry.getKey(), -entry.getValue(), Integer::sum);
        }
    }

    @Override
    public List<Resource> takeResources(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        List<Resource> taken = new ArrayList<>();
        Map<Resource, Integer> needed = count(resources);
        for (Map.Entry<Resource, Integer> entry : needed.entrySet()) {
            Resource r = entry.getKey();
            int want = entry.getValue();
            int have = stock.getOrDefault(r, 0);
            int actual = Math.min(want, have);
            stock.put(r, have - actual);
            for (int i = 0; i < actual; i++) {
                taken.add(r);
            }
        }
        return taken;
    }

    @Override
    public void putResources(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        for (Resource r : resources) {
            Objects.requireNonNull(r, "resource element must not be null");
            stock.merge(r, 1, Integer::sum);
            assert stock.get(r) <= ResourceMarket.MAX_RESOURCES.get(r);
        }
    }

    @Override
    public String getStateString() {
        return "{\"coal\":" + stock.getOrDefault(Resource.COAL, 0)
                + ",\"oil\":" + stock.getOrDefault(Resource.OIL, 0)
                + ",\"garbage\":" + stock.getOrDefault(Resource.GARBAGE, 0)
                + ",\"uranium\":" + stock.getOrDefault(Resource.URANIUM, 0) + "}";
    }

    @Override
    public String toString() {
        return getStateString();
    }

    private static Map<Resource, Integer> count(final List<Resource> list) {
        Map<Resource, Integer> map = new EnumMap<>(Resource.class);
        for (Resource r : list) {
            Objects.requireNonNull(r, "resource element must not be null");
            map.merge(r, 1, Integer::sum);
        }
        return map;
    }
}
