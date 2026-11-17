package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles calculating the turn order of players for game rounds and determining
 * the final game rankings at the end of the game according to Power Grid rules.
 */
public final class DeterminePlayerOrder {

    private final List<PlayerColor> players;
    private final InterfaceTownsDeterminePlayerOrder towns;
    private final Map<PlayerColor, InterfacePlayerBoardDeterminePlayerOrder> playerBoards;
    private List<PlayerColor> currentOrder;

    /**
     * Constructs a DeterminePlayerOrder manager with the given initial player order,
     * towns query interface, and mapping of players to their player board facades.
     *
     * @param initialPlayerOrder the initial order of players
     * @param towns              the towns query interface for connected cities
     * @param playerBoards       mapping of each player color to their player board facade interface
     */
    public DeterminePlayerOrder(
            final List<PlayerColor> initialPlayerOrder,
            final InterfaceTownsDeterminePlayerOrder towns,
            final Map<PlayerColor, InterfacePlayerBoardDeterminePlayerOrder> playerBoards) {
        Objects.requireNonNull(initialPlayerOrder, "Initial player order cannot be null");
        this.players = List.copyOf(initialPlayerOrder);
        this.towns = Objects.requireNonNull(towns, "Towns cannot be null");
        this.playerBoards = Map.copyOf(Objects.requireNonNull(playerBoards, "PlayerBoards cannot be null"));
        this.currentOrder = new ArrayList<>(this.players);
    }

    /**
     * Reorders the players according to Phase 1 (Determine Player Order) rules:
     * 1. Player with the most connected cities in their network is first.
     * 2. Ties are broken by the player with the highest-numbered power plant.
     * 3. Remaining ties preserve the relative player order from the previous round.
     *
     * @return the newly ordered list of players from first to last
     */
    public List<PlayerColor> reorder() {
        List<PlayerColor> sorted = new ArrayList<>(currentOrder);
        sorted.sort((p1, p2) -> {
            int cities1 = towns.getNumberOfHouses(p1);
            int cities2 = towns.getNumberOfHouses(p2);
            if (cities1 != cities2) {
                return Integer.compare(cities2, cities1);
            }

            int ppVal1 = getBestPowerPlantValue(p1);
            int ppVal2 = getBestPowerPlantValue(p2);
            if (ppVal1 != ppVal2) {
                return Integer.compare(ppVal2, ppVal1);
            }

            return Integer.compare(currentOrder.indexOf(p1), currentOrder.indexOf(p2));
        });

        this.currentOrder = sorted;
        return Collections.unmodifiableList(new ArrayList<>(currentOrder));
    }

    /**
     * Determines the final game rankings according to the end-of-game rules:
     * 1. The player who supplies electricity to the most cities in their network wins.
     * 2. Ties are broken by the player with the most remaining money (Elektro).
     * 3. Secondary ties are broken by the player with the most connected cities in their network.
     * 4. Tertiary ties are broken by the player with the highest-numbered power plant.
     * 5. Remaining ties preserve the current player order.
     *
     * @return list of players ordered from first place (winner) to last place
     */
    public List<PlayerColor> gameResult() {
        List<PlayerColor> sorted = new ArrayList<>(currentOrder);
        sorted.sort((p1, p2) -> {
            int supplied1 = getSuppliedCities(p1);
            int supplied2 = getSuppliedCities(p2);
            if (supplied1 != supplied2) {
                return Integer.compare(supplied2, supplied1);
            }

            int money1 = getMoney(p1);
            int money2 = getMoney(p2);
            if (money1 != money2) {
                return Integer.compare(money2, money1);
            }

            int cities1 = towns.getNumberOfHouses(p1);
            int cities2 = towns.getNumberOfHouses(p2);
            if (cities1 != cities2) {
                return Integer.compare(cities2, cities1);
            }

            int ppVal1 = getBestPowerPlantValue(p1);
            int ppVal2 = getBestPowerPlantValue(p2);
            if (ppVal1 != ppVal2) {
                return Integer.compare(ppVal2, ppVal1);
            }

            return Integer.compare(currentOrder.indexOf(p1), currentOrder.indexOf(p2));
        });

        return Collections.unmodifiableList(sorted);
    }

    private int getSuppliedCities(final PlayerColor player) {
        int connectedCities = towns.getNumberOfHouses(player);
        InterfacePlayerBoardDeterminePlayerOrder board = playerBoards.get(player);
        int connectableCapacity = board != null ? board.numberOfConnectableCities() : 0;
        return Math.min(connectedCities, connectableCapacity);
    }

    private int getMoney(final PlayerColor player) {
        InterfacePlayerBoardDeterminePlayerOrder board = playerBoards.get(player);
        return board != null ? board.outstandingMoney() : 0;
    }

    private int getBestPowerPlantValue(final PlayerColor player) {
        InterfacePlayerBoardDeterminePlayerOrder board = playerBoards.get(player);
        if (board == null) {
            return -1;
        }
        PowerPlantNumber best = board.bestPowerPlantNumber();
        return best != null ? best.getValue() : -1;
    }
}
