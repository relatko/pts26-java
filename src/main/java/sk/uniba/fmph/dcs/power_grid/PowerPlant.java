package sk.uniba.fmph.dcs.power_grid;
import java.util.List;
import java.util.Objects;

public record PowerPlant(PowerPlantNumber number, List<Resource> acceptedResources, int resourceAmount, int numberOfCities) {
    public PowerPlant {
        Objects.requireNonNull(number);
        acceptedResources = List.copyOf(acceptedResources);

        if (resourceAmount < 0 || numberOfCities < 1) {
            throw new IllegalArgumentException("Invalid power plant resource amount or capacity");
        }
    }
}
