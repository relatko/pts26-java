package sk.uniba.fmph.dcs.power_grid;
import java.util.List;

public interface AuctionPowerPlantsPort {
    void startPhase(List<PlayerColor> playerOrder);
    Variant<PhaseStatus, RequestError> selectPowerPlantToBid(PlayerColor player, PowerPlantNumber number, int firstBid);
    Variant<PhaseStatus, RequestError> doNotWantPowerPlant(PlayerColor player);
    Variant<PhaseStatus, RequestError> bidForCurrentPowerPlant(PlayerColor player, int bid);
    Variant<PhaseStatus, RequestError> doNotBidForCurrentPowerPlant(PlayerColor player);
    Variant<PhaseStatus, RequestError> removePowerPlant(PlayerColor player, PowerPlantNumber number, List<Resource> removedResources);
    String getState();
}

