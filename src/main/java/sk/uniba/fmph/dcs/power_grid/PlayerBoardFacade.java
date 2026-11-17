package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Facade aggregating the subsystems of a player's board:
 * PlayerMoney, PlayerPowerPlants, and PlayerResources.
 */
public final class PlayerBoardFacade implements InterfacePlayerBoardDeterminePlayerOrder, InterfacePlayerBoardFacade {

    private final PlayerColor playerColor;
    private final InterfacePlayerMoney playerMoney;
    private final InterfacePlayerPowerPlants playerPowerPlants;
    private final InterfacePlayerResources playerResources;
    private final InterfaceTownsPlayerBoard towns;

    /**
     * Constructs a PlayerBoardFacade with full subsystem dependencies.
     *
     * @param playerColor       the player color identifying this board
     * @param playerMoney       the player money subsystem
     * @param playerPowerPlants the player power plants subsystem
     * @param playerResources   the player resources subsystem
     * @param towns             the towns query interface for connected cities
     */
    public PlayerBoardFacade(
            final PlayerColor playerColor,
            final InterfacePlayerMoney playerMoney,
            final InterfacePlayerPowerPlants playerPowerPlants,
            final InterfacePlayerResources playerResources,
            final InterfaceTownsPlayerBoard towns) {
        this.playerColor = Objects.requireNonNull(playerColor, "PlayerColor cannot be null");
        this.playerMoney = Objects.requireNonNull(playerMoney, "PlayerMoney cannot be null");
        this.playerPowerPlants = Objects.requireNonNull(playerPowerPlants, "PlayerPowerPlants cannot be null");
        this.playerResources = Objects.requireNonNull(playerResources, "PlayerResources cannot be null");
        this.towns = towns;
    }

    /**
     * Constructs a PlayerBoardFacade without a towns query reference.
     *
     * @param playerColor       the player color identifying this board
     * @param playerMoney       the player money subsystem
     * @param playerPowerPlants the player power plants subsystem
     * @param playerResources   the player resources subsystem
     */
    public PlayerBoardFacade(
            final PlayerColor playerColor,
            final InterfacePlayerMoney playerMoney,
            final InterfacePlayerPowerPlants playerPowerPlants,
            final InterfacePlayerResources playerResources) {
        this(playerColor, playerMoney, playerPowerPlants, playerResources, null);
    }

    public PlayerColor getPlayerColor() {
        return playerColor;
    }

    @Override
    public PowerPlantNumber bestPowerPlantNumber() {
        return playerPowerPlants.bestPowerPlant();
    }

    @Override
    public int outstandingMoney() {
        return playerMoney.currentMoney();
    }

    @Override
    public boolean hasEnoughMoney(final int amount) {
        return playerMoney.hasEnoughMoney(amount);
    }

    @Override
    public void spendMoney(final int amount) {
        playerMoney.spendMoney(amount);
    }

    @Override
    public int numberOfConnectableCities() {
        return playerResources.numberOfConnectableCities();
    }

    @Override
    public int numberOfConnectedCities() {
        return towns != null ? towns.getNumberOfHouses(playerColor) : 0;
    }

    /**
     * Receives a power plant by number after paying the specified price.
     * Deducts the price from player money if the player has enough money.
     *
     * @param ppn   the power plant number
     * @param price the purchase price
     * @return true if the plant was received and the player does not exceed the limit of 3;
     *         false if the player is above the power plant limit (owns 4) or cannot pay
     */
    public boolean receivePowerPlant(final PowerPlantNumber ppn, final int price) {
        Objects.requireNonNull(ppn, "PowerPlantNumber cannot be null");
        if (price > 0) {
            if (!playerMoney.hasEnoughMoney(price)) {
                return false;
            }
            playerMoney.spendMoney(price);
        }
        return playerPowerPlants.receivePowerPlant(ppn);
    }

    /**
     * Receives a power plant card after paying the specified price.
     *
     * @param powerPlant the power plant
     * @param price      the purchase price
     * @return true if the plant was received and player is within limit;
     *         false if player is above the power plant limit
     */
    public boolean receivePowerPlant(final PowerPlant powerPlant, final int price) {
        Objects.requireNonNull(powerPlant, "PowerPlant cannot be null");
        if (price > 0) {
            if (!playerMoney.hasEnoughMoney(price)) {
                return false;
            }
            playerMoney.spendMoney(price);
        }
        return playerPowerPlants.receivePowerPlant(powerPlant);
    }

    /**
     * Scraps an owned power plant and manages reassignment of remaining resources.
     *
     * @param ppn              the power plant number to remove
     * @param removedResources resources from the scrapped power plant
     * @return Optional.empty() on success, or RequestError if removal fails
     */
    public Optional<RequestError> removePowerPlant(
            final PowerPlantNumber ppn,
            final List<Resource> removedResources) {
        Objects.requireNonNull(ppn, "PowerPlantNumber cannot be null");
        Objects.requireNonNull(removedResources, "Removed resources cannot be null");

        Optional<RequestError> plantError = playerPowerPlants.removePowerPlant(ppn, removedResources);
        if (plantError.isPresent()) {
            return plantError;
        }
        return playerResources.removePowerPlant(ppn, removedResources);
    }

    /**
     * Buys resources and stores them on owned power plants.
     *
     * @param resources the resources to buy
     * @return Optional.empty() on success, or RequestError on validation failure
     */
    public Optional<RequestError> buyResources(final List<Resource> resources) {
        Objects.requireNonNull(resources, "Resources cannot be null");
        return playerResources.buyResources(resources);
    }

    /**
     * Uses power plants and consumes resources to power cities during Bureaucracy.
     *
     * @param player           the player color
     * @param ppns             the power plant numbers to fire
     * @param resourcesToBurn  the resources to burn
     * @return Optional.empty() on success, or RequestError on failure
     */
    public Optional<RequestError> usePowerPlants(
            final PlayerColor player,
            final List<PowerPlantNumber> ppns,
            final List<Resource> resourcesToBurn) {
        Objects.requireNonNull(player, "PlayerColor cannot be null");
        Objects.requireNonNull(ppns, "PowerPlantNumbers cannot be null");
        Objects.requireNonNull(resourcesToBurn, "Resources to burn cannot be null");

        if (!this.playerColor.equals(player)) {
            return Optional.of(new RequestError("Player color mismatch: expected "
                    + this.playerColor + " but received " + player));
        }

        return playerResources.usePowerPlants(player, ppns, resourcesToBurn);
    }

    /**
     * Returns a JSON-formatted state string representing this player board.
     *
     * @return serialized state string
     */
    public String state() {
        PowerPlantNumber best = bestPowerPlantNumber();
        return String.format(
                "{\"player\":\"%s\",\"money\":%d,\"bestPowerPlant\":%s,\"connectableCities\":%d,\"connectedCities\":%d}",
                playerColor.name(),
                outstandingMoney(),
                best != null ? best.toString() : "null",
                numberOfConnectableCities(),
                numberOfConnectedCities()
        );
    }
}
