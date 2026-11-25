package sk.uniba.fmph.dcs.power_grid;
import java.util.List;
import java.util.Optional;

public interface PowerPlantMarketPort {
    List<PowerPlant> currentMarket();
    List<PowerPlant> futureMarket();
    void startAuction();
    Optional<RequestError> canTakePowerPlant(PowerPlantNumber number, int price);
    PowerPlant takePowerPlant(PowerPlantNumber number);
    void removeDiscountedPowerPlant();
    void removePowerPlantsCheaperThan(int value);
    void prepareNextTurn();
    void startStep2();
    boolean isStep3Start();
    void startStep3();
}
