package sk.uniba.fmph.dcs.power_grid;
import java.util.List;

public interface PowerGridRulesGameInterface {
    Variant<SerializedGameState, RequestError> selectPowerPlantToBid(int playerId, int powerPlantNumber, int firstBid);
    Variant<SerializedGameState, RequestError> doNotWantPowerPlant(int playerId);
    Variant<SerializedGameState, RequestError> bidForCurrentPowerPlant(int playerId, int bid);
    Variant<SerializedGameState, RequestError> doNotBidForCurrentPowerPlant(int playerId);
    Variant<SerializedGameState, RequestError> removePowerPlant(int playerId, int powerPlantNumber, String removedResources);
    Variant<SerializedGameState, RequestError> buyResources(int playerId, String resources);
    Variant<SerializedGameState, RequestError> buyHouses(int playerId, List<String> townNames);
    Variant<SerializedGameState, RequestError> usePowerPlants(int playerId, List<Integer> powerPlantNumbers, String resourcesToBurn);
}

