package sk.uniba.fmph.dcs.power_grid;

/**
 * Interface for PlayerBoardFacade as seen by BuildingHouses.
 */
public interface InterfacePlayerBoardFacade {
    /** Returns true if the player has at least the given amount of money. */
    boolean hasEnoughMoney(int amount);

    /** Deducts the given amount from the player's treasury. */
    void spendMoney(int amount);

    /** Returns the number of cities this player has connected so far. */
    int numberOfConnectedCities();
}
