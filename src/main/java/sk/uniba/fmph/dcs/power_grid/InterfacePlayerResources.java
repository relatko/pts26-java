package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Optional;

/**
 * Interface representing PlayerResources functionality for PlayerBoardFacade.
 */
public interface InterfacePlayerResources {

    Optional<RequestError> buyResources(List<Resource> resources);

    Optional<RequestError> usePowerPlants(
            PlayerColor playerColor,
            List<PowerPlantNumber> powerPlantNumbers,
            List<Resource> resourcesToBurn);

    int numberOfConnectableCities();

    Optional<RequestError> removePowerPlant(PowerPlantNumber powerPlantNumber, List<Resource> remainingResources);

    List<Resource> getResources();
}
