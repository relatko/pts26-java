package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Optional;

/**
 * Interface representing PlayerPowerPlants functionality for PlayerBoardFacade.
 */
public interface InterfacePlayerPowerPlants {

    PowerPlantNumber bestPowerPlant();

    boolean receivePowerPlant(PowerPlant powerPlant);

    boolean receivePowerPlant(PowerPlantNumber powerPlantNumber);

    Optional<RequestError> removePowerPlant(PowerPlantNumber powerPlantNumber, List<Resource> remainingResources);

    int numberOfConnectableCities(List<Resource> resources);

    boolean canStoreResources(List<Resource> resources);

    List<PowerPlant> getPowerPlants();
}
