package sk.uniba.fmph.dcs.power_grid;

import java.util.List;

/**
 * Interface for the Towns component as seen by BuildingHouses.
 * Provides the ability to purchase houses in a list of towns for a player.
 */
public interface InterfaceTownsBuildingHouses {
    /**
     * Attempts to buy houses in the given towns for the given player.
     *
     * @param player    the player buying houses
     * @param townNames list of target town names
     * @return total cost as {@link Integer}, or {@link RequestError} on failure
     */
    Object buyHouses(PlayerColor player, List<String> townNames);
}
