package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResourceMarket {

    public static final Map<Resource, Integer> MAX_RESOURCES = Map.of(Resource.COAL, 24, Resource.OIL, 24,
            Resource.GARBAGE, 24, Resource.URANIUM, 12);

    private int step;
    private final Map<Resource, Integer> resources = new EnumMap<>(Resource.class);
    private final InterfaceResourcePoolResourceMarket pool;
    private final Map<Resource, List<Integer>> expandedPrices = new EnumMap<>(Resource.class);
    private final Map<Integer, Map<Resource, Integer>> refillingPattern = new HashMap<>();

    public ResourceMarket(final InterfaceResourcePoolResourceMarket pool, final List<Resource> startingResources,
            final Map<Integer, Map<Resource, Integer>> refillingPattern) {
        this.step = 1;
        this.pool = pool;

        for (Resource resource : Resource.values()) {
            int slots = MAX_RESOURCES.get(resource);
            List<Integer> prices = new ArrayList<>(slots);
            if (resource == Resource.URANIUM) {
                for (int i = 1; i <= 8; i++) {
                    prices.add(i);
                }
                prices.add(10);
                prices.add(12);
                prices.add(14);
                prices.add(16);
            } else {
                for (int i = 1; i <= 8; i++) {
                    for (int repeat = 0; repeat < 3; repeat++) {

                        prices.add(i);
                    }
                }
            }
            assert prices.size() == slots;
            expandedPrices.put(resource, prices);
        }

        Map<Resource, Integer> startingCounts = count(startingResources);
        for (Resource resource : Resource.values()) {
            int count = startingCounts.getOrDefault(resource, 0);
            assert count >= 0 && count <= MAX_RESOURCES.get(resource);
            resources.put(resource, count);
        }

        assert refillingPattern.size() == 3;
        for (int s = 1; s <= 3; s++) {
            Map<Resource, Integer> pattern = new EnumMap<>(Resource.class);
            pattern.putAll(refillingPattern.get(s));
            assert pattern.size() == 4;
            for (Resource resource : Resource.values()) {
                int value = pattern.get(resource);
                assert value >= 0 && value <= MAX_RESOURCES.get(resource);
            }
            this.refillingPattern.put(s, pattern);
        }
    }

    public void startStep2() {
        assert step == 1;
        step = 2;
    }

    public void startStep3() {
        assert step < 3;
        step = 3;
    }

    public void refill() {
        for (Resource resource : Resource.values()) {
            int refillCount = refillingPattern.get(step).get(resource);
            List<Resource> request = new ArrayList<>(refillCount);
            for (int i = 0; i < refillCount; i++) {
                request.add(resource);
            }
            List<Resource> fromPool = pool.takeResources(request);
            for (Resource r : fromPool) {
                assert r == resource;
            }
            resources.merge(resource, fromPool.size(), Integer::sum);
            assert resources.get(resource) <= MAX_RESOURCES.get(resource);
        }
    }

    public Integer getPrice(final List<Resource> desired) {
        int totalPrice = 0;
        List<Resource> unavailable = new ArrayList<>();
        Map<Resource, Integer> desiredCounts = count(desired);

        for (Resource resource : Resource.values()) {
            int required = desiredCounts.getOrDefault(resource, 0);
            int available = resources.get(resource);
            List<Integer> prices = expandedPrices.get(resource);

            if (required <= available) {
                int from = prices.size() - available;
                totalPrice += sum(prices, from, from + required);
            } else {
                totalPrice += sum(prices, prices.size() - available, prices.size());
                int fromStorage = required - available;
                totalPrice += fromStorage * prices.get(prices.size() - 1);
                for (int i = 0; i < fromStorage; i++) {
                    unavailable.add(resource);
                }
            }
        }

        if (unavailable.isEmpty()) {
            return totalPrice;
        }
        if (pool.canTakeResourcesFromStorageSpace(unavailable)) {
            return totalPrice;
        }
        return null;
    }

    public void takeResources(final List<Resource> resourcesToTake) {
        Map<Resource, Integer> toTakeCounts = count(resourcesToTake);
        for (Resource resource : Resource.values()) {
            int toTake = toTakeCounts.getOrDefault(resource, 0);
            int fromStorage = toTake - resources.get(resource);
            if (fromStorage > 0) {
                List<Resource> storage = new ArrayList<>(fromStorage);
                for (int i = 0; i < fromStorage; i++) {
                    storage.add(resource);
                }
                pool.getFromStorageSpace(storage);
                resources.put(resource, 0);
            } else {
                resources.merge(resource, -toTake, Integer::sum);
            }
        }
    }

    @Override
    public String toString() {
        return "{\"coal\":" + resources.get(Resource.COAL) + ",\"oil\":" + resources.get(Resource.OIL) + ",\"garbage\":"
                + resources.get(Resource.GARBAGE) + ",\"uranium\":" + resources.get(Resource.URANIUM) + ",\"pool\":"
                + pool.toString() + "}";
    }

    private static Map<Resource, Integer> count(final List<Resource> list) {
        Map<Resource, Integer> counts = new EnumMap<>(Resource.class);
        for (Resource resource : list) {
            counts.merge(resource, 1, Integer::sum);
        }
        return counts;
    }

    private static int sum(final List<Integer> list, final int from, final int to) {
        int total = 0;
        for (int i = from; i < to; i++) {
            total += list.get(i);
        }
        return total;
    }
}
