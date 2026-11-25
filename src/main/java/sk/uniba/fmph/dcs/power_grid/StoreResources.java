package sk.uniba.fmph.dcs.power_grid;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Decides whether a set of power plants can store a given collection of resources. */
public final class StoreResources {

    private static final int STORAGE_MULTIPLIER = 2;

    /**
     * Checks whether all resources fit in the plants at the same time.
     *
     * By Hall's theorem, a valid distribution exists if and only if, for every subset of resource types, the
     * resources of those types do not exceed the total capacity of the plants accepting at least one of them.
     * This holds for any combination of accepted types, so hybrid plants need no special treatment.
     *
     * @param resources
     *            resources to store
     * @param powerplants
     *            available power plants
     * @return true if every resource can be stored
     */
    public boolean canStoreResources(final List<Resource> resources, final List<PowerPlant> powerplants) {
        Resource[] types = Resource.values();
        for (int mask = 1; mask < 1 << types.length; mask++) {
            Set<Resource> subset = subsetOf(types, mask);
            long required = resources.stream().filter(subset::contains).count();
            long capacity = powerplants.stream().filter(plant -> acceptsAny(plant, subset))
                    .mapToLong(StoreResources::capacityOf).sum();
            if (required > capacity) {
                return false;
            }
        }
        return true;
    }

    private static Set<Resource> subsetOf(final Resource[] types, final int mask) {
        Set<Resource> subset = EnumSet.noneOf(Resource.class);
        for (Resource type : types) {
            if ((mask & 1 << type.ordinal()) != 0) {
                subset.add(type);
            }
        }
        return subset;
    }

    private static boolean acceptsAny(final PowerPlant plant, final Set<Resource> subset) {
        return plant.acceptedResources().stream().anyMatch(subset::contains);
    }

    private static long capacityOf(final PowerPlant plant) {
        return (long) STORAGE_MULTIPLIER * plant.resourceAmount();
    }
}
