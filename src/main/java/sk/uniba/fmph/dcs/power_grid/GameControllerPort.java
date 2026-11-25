package sk.uniba.fmph.dcs.power_grid;
import java.util.List;
import java.util.Optional;

public interface GameControllerPort {
    Optional<RequestError> selectPowerPlantToBid(PlayerColor player, PowerPlantNumber number, int firstBid);
    Optional<RequestError> doNotWantPowerPlant(PlayerColor player);
    Optional<RequestError> bidForCurrentPowerPlant(PlayerColor player, int bid);
    Optional<RequestError> doNotBidForCurrentPowerPlant(PlayerColor player);
    Optional<RequestError> removePowerPlant(PlayerColor player, PowerPlantNumber number, List<Resource> removedResources);
    Optional<RequestError> buyResources(PlayerColor player, List<Resource> resources);
    Optional<RequestError> buyHouses(PlayerColor player, List<String> townNames);
    Optional<RequestError> usePowerPlants(PlayerColor player, List<PowerPlantNumber> numbers, List<Resource> resourcesToBurn);
    String getState();
}
