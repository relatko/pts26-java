package sk.uniba.fmph.dcs.power_grid;

/**
 * Interface representing the subset of PlayerBoardFacade functionality
 * required by DeterminePlayerOrder.
 */
public interface InterfacePlayerBoardDeterminePlayerOrder {

    /**
     * Returns the highest-numbered power plant owned by the player,
     * or null if the player owns no power plants.
     *
     * @return the highest-numbered power plant, or null
     */
    PowerPlantNumber bestPowerPlantNumber();

    /**
     * Returns the number of cities this player's power plants and resources
     * are capable of supplying with electricity.
     *
     * @return maximum suppliable cities count
     */
    int numberOfConnectableCities();

    /**
     * Returns the current amount of money owned by the player.
     *
     * @return current Elektro cash
     */
    int outstandingMoney();
}
