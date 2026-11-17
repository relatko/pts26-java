package sk.uniba.fmph.dcs.power_grid;

/**
 * Interface representing the subset of Towns functionality required by DeterminePlayerOrder.
 */
public interface InterfaceTownsDeterminePlayerOrder {

    /**
     * Returns the number of houses built / cities connected by the specified player.
     *
     * @param player the player whose connected cities count is queried
     * @return the number of connected cities for the player
     */
    int getNumberOfHouses(PlayerColor player);
}
