package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Optional;

public interface PlayerBoardPort {
    PowerPlantNumber bestPowerPlantNumber();
    int outstandingMoney();
    void spendMoney(int amount);
    int numberOfConnectableCities();
    boolean hasEnoughMoney(int amount);

    // Returns false if the player must remove an old plant after the purchase.
    boolean receivePowerPlant(PowerPlant plant, int price);

    Optional<RequestError> removePowerPlant(PowerPlantNumber number, List<Resource> removedResources);
    Optional<RequestError> buyResources(List<Resource> resources);
    Variant<Integer, RequestError> usePowerPlants(List<PowerPlantNumber> numbers, List<Resource> resourcesToBurn);
    void receiveMoney(int numberOfTowns);
    String state();
}
