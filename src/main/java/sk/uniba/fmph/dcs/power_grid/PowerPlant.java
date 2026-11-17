package sk.uniba.fmph.dcs.power_grid;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Power Plant card in Power Grid.
 */
public final class PowerPlant {

    private final PowerPlantNumber number;
    private final List<Resource> acceptedResources;
    private final int resourceAmount;
    private final int numberOfCities;

    public PowerPlant(
            final PowerPlantNumber number,
            final List<Resource> acceptedResources,
            final int resourceAmount,
            final int numberOfCities) {
        this.number = Objects.requireNonNull(number, "PowerPlantNumber cannot be null");
        this.acceptedResources = acceptedResources == null ? Collections.emptyList() : List.copyOf(acceptedResources);
        this.resourceAmount = resourceAmount;
        this.numberOfCities = numberOfCities;
    }

    public PowerPlantNumber getNumber() {
        return number;
    }

    public List<Resource> getAcceptedResources() {
        return acceptedResources;
    }

    public int getResourceAmount() {
        return resourceAmount;
    }

    public int getNumberOfCities() {
        return numberOfCities;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof PowerPlant otherPlant)) {
            return false;
        }
        return resourceAmount == otherPlant.resourceAmount
                && numberOfCities == otherPlant.numberOfCities
                && Objects.equals(number, otherPlant.number)
                && Objects.equals(acceptedResources, otherPlant.acceptedResources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, acceptedResources, resourceAmount, numberOfCities);
    }

    @Override
    public String toString() {
        return "PowerPlant{"
                + "number=" + number
                + ", acceptedResources=" + acceptedResources
                + ", resourceAmount=" + resourceAmount
                + ", numberOfCities=" + numberOfCities
                + '}';
    }
}
