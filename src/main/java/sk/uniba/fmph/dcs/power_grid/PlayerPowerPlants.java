package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Manages a player's owned power plants (maximum 3) and validates resource storage,
 * maximum suppliable cities, and power plant activation during Bureaucracy.
 */
public final class PlayerPowerPlants {
    private static final int MAX_POWER_PLANTS = 3;

    private final List<PowerPlant> powerPlants = new ArrayList<>();

    /**
     * Returns the {@link PowerPlantNumber} of the highest-numbered owned power plant,
     * or {@code null} if the player owns no power plants.
     */
    public PowerPlantNumber bestPowerPlant() {
        if (powerPlants.isEmpty()) {
            return null;
        }
        PowerPlant best = powerPlants.get(0);
        for (PowerPlant pp : powerPlants) {
            if (pp.getNumber().getNumber() > best.getNumber().getNumber()) {
                best = pp;
            }
        }
        return best.getNumber();
    }

    /**
     * Adds a power plant to the player's collection.
     *
     * @param pp the power plant to add
     * @return {@code false} if the player already owns 3 power plants (or already owns this plant),
     *         {@code true} otherwise
     */
    public boolean receivePowerPlant(final PowerPlant pp) {
        Objects.requireNonNull(pp, "pp must not be null");
        if (powerPlants.size() >= MAX_POWER_PLANTS) {
            return false;
        }
        if (findByNumber(pp.getNumber()) != null) {
            return false;
        }
        powerPlants.add(pp);
        return true;
    }

    /**
     * Removes the power plant with the specified number if the remaining power plants
     * can store {@code newResources}.
     *
     * @param ppn          number of the power plant to remove
     * @param newResources resources remaining after discarding excess fuel
     * @return {@link Optional#empty()} on success, or {@link Optional} containing a {@link RequestError}
     */
    public Optional<RequestError> removePowerPlant(final PowerPlantNumber ppn,
                                                   final List<Resource> newResources) {
        if (ppn == null || newResources == null) {
            return Optional.of(new RequestError("Power plant number and resources must not be null."));
        }
        PowerPlant toRemove = findByNumber(ppn);
        if (toRemove == null) {
            return Optional.of(new RequestError("Power plant " + ppn + " not found."));
        }

        List<PowerPlant> remainingPlants = new ArrayList<>();
        for (PowerPlant pp : powerPlants) {
            if (pp != toRemove) {
                remainingPlants.add(pp);
            }
        }

        if (!canStoreInPlants(newResources, remainingPlants)) {
            return Optional.of(new RequestError("Remaining power plants cannot store the specified resources."));
        }

        powerPlants.remove(toRemove);
        return Optional.empty();
    }

    /**
     * Computes the maximum number of cities the player can power given {@code resources}.
     * Evaluates all subsets of owned power plants so hybrid and overlapping-fuel plants
     * are utilized optimally.
     *
     * @param resources available resource tokens
     * @return maximum number of cities that can be supplied with electricity
     */
    public int numberOfConnectableCities(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        Map<Resource, Integer> availableCounts = countResources(resources);
        int n = powerPlants.size();
        int maxCities = 0;

        for (int mask = 1; mask < (1 << n); mask++) {
            List<PowerPlant> subset = new ArrayList<>();
            int cities = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    PowerPlant pp = powerPlants.get(i);
                    subset.add(pp);
                    cities += pp.getNumberOfCities();
                }
            }
            if (cities > maxCities && canSupplyPlants(subset, 0, availableCounts, false)) {
                maxCities = cities;
            }
        }
        return maxCities;
    }

    /**
     * Checks whether the player's current power plants can store all tokens in {@code resources}.
     * Each plant can store up to {@code 2 * resourceAmount} tokens of its accepted resource types
     * (hybrid plants share capacity across their accepted types; ecological plants store 0).
     *
     * @param resources full list of resources to store
     * @return {@code true} if a valid storage assignment exists, {@code false} otherwise
     */
    public boolean canStoreResources(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        return canStoreInPlants(resources, powerPlants);
    }

    /**
     * Validates and executes powering the selected power plants using {@code resourcesToBurn}.
     * Every selected plant must be owned, unique, and supplied with its exact required resource count,
     * and all tokens in {@code resourcesToBurn} must be consumed.
     *
     * @param ppns            power plant numbers to activate
     * @param resourcesToBurn exact resources burned to run the selected plants
     * @return {@link Integer} number of cities powered on success, or {@link RequestError} on failure
     */
    public Object usePowerPlant(final List<PowerPlantNumber> ppns,
                                final List<Resource> resourcesToBurn) {
        if (ppns == null || resourcesToBurn == null) {
            return new RequestError("Power plants and resources must not be null.");
        }

        List<PowerPlant> selectedPlants = new ArrayList<>();
        Set<PowerPlantNumber> seen = new HashSet<>();
        for (PowerPlantNumber ppn : ppns) {
            if (ppn == null) {
                return new RequestError("Power plant number must not be null.");
            }
            if (!seen.add(ppn)) {
                return new RequestError("Duplicate power plant " + ppn + " cannot be used twice.");
            }
            PowerPlant pp = findByNumber(ppn);
            if (pp == null) {
                return new RequestError("Power plant " + ppn + " not found.");
            }
            selectedPlants.add(pp);
        }

        int totalRequiredTokens = 0;
        int totalCities = 0;
        for (PowerPlant pp : selectedPlants) {
            totalRequiredTokens += pp.getResourceAmount();
            totalCities += pp.getNumberOfCities();
        }

        if (resourcesToBurn.size() > totalRequiredTokens) {
            return new RequestError("Excess resources specified.");
        }
        if (resourcesToBurn.size() < totalRequiredTokens) {
            return new RequestError("Not enough resources specified.");
        }

        Map<Resource, Integer> burnCounts = countResources(resourcesToBurn);
        if (!canSupplyPlants(selectedPlants, 0, burnCounts, true)) {
            return new RequestError("Specified resources cannot supply the selected power plants.");
        }

        return totalCities;
    }

    public List<PowerPlant> getPowerPlants() {
        return Collections.unmodifiableList(new ArrayList<>(powerPlants));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < powerPlants.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(powerPlants.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    // ----- Private helpers -----

    private PowerPlant findByNumber(final PowerPlantNumber ppn) {
        for (PowerPlant pp : powerPlants) {
            if (pp.getNumber().equals(ppn)) {
                return pp;
            }
        }
        return null;
    }

    private static boolean canStoreInPlants(final List<Resource> resources,
                                            final List<PowerPlant> plants) {
        if (resources.isEmpty()) {
            return true;
        }
        Map<Resource, Integer> needed = countResources(resources);
        Resource[] allTypes = Resource.values();
        int numTypes = allTypes.length;

        // Hall's Marriage Theorem: for every non-empty subset of resource types,
        // total tokens of those types must not exceed total capacity of plants accepting at least one of them.
        for (int mask = 1; mask < (1 << numTypes); mask++) {
            int requiredTokens = 0;
            for (int i = 0; i < numTypes; i++) {
                if ((mask & (1 << i)) != 0) {
                    requiredTokens += needed.getOrDefault(allTypes[i], 0);
                }
            }
            if (requiredTokens == 0) {
                continue;
            }

            int availableCapacity = 0;
            for (PowerPlant pp : plants) {
                if (acceptsAnyInMask(pp, allTypes, mask)) {
                    availableCapacity += pp.getResourceAmount() * 2;
                }
            }
            if (requiredTokens > availableCapacity) {
                return false;
            }
        }
        return true;
    }

    private static boolean acceptsAnyInMask(final PowerPlant pp,
                                            final Resource[] allTypes,
                                            final int mask) {
        List<Resource> accepted = pp.getAcceptedResources();
        for (int i = 0; i < allTypes.length; i++) {
            if ((mask & (1 << i)) != 0 && accepted.contains(allTypes[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSupplyPlants(final List<PowerPlant> plants,
                                           final int plantIndex,
                                           final Map<Resource, Integer> remaining,
                                           final boolean requireExactConsumption) {
        if (plantIndex == plants.size()) {
            if (!requireExactConsumption) {
                return true;
            }
            for (int count : remaining.values()) {
                if (count != 0) {
                    return false;
                }
            }
            return true;
        }

        PowerPlant pp = plants.get(plantIndex);
        int amount = pp.getResourceAmount();
        List<Resource> accepted = pp.getAcceptedResources();

        if (amount == 0) {
            return canSupplyPlants(plants, plantIndex + 1, remaining, requireExactConsumption);
        }
        if (accepted.isEmpty()) {
            return false;
        }

        return assignTokensForPlant(plants, plantIndex, accepted, 0, amount, remaining, requireExactConsumption);
    }

    private static boolean assignTokensForPlant(final List<PowerPlant> plants,
                                                final int plantIndex,
                                                final List<Resource> accepted,
                                                final int typeIndex,
                                                final int tokensLeft,
                                                final Map<Resource, Integer> remaining,
                                                final boolean requireExactConsumption) {
        if (tokensLeft == 0) {
            return canSupplyPlants(plants, plantIndex + 1, remaining, requireExactConsumption);
        }
        if (typeIndex >= accepted.size()) {
            return false;
        }

        Resource currentType = accepted.get(typeIndex);
        int available = remaining.getOrDefault(currentType, 0);
        int maxUse = Math.min(tokensLeft, available);

        for (int use = maxUse; use >= 0; use--) {
            remaining.put(currentType, available - use);
            if (assignTokensForPlant(plants, plantIndex, accepted, typeIndex + 1,
                    tokensLeft - use, remaining, requireExactConsumption)) {
                remaining.put(currentType, available);
                return true;
            }
        }
        remaining.put(currentType, available);
        return false;
    }

    private static Map<Resource, Integer> countResources(final List<Resource> list) {
        Map<Resource, Integer> counts = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            counts.put(r, 0);
        }
        for (Resource r : list) {
            counts.merge(r, 1, Integer::sum);
        }
        return counts;
    }
}
