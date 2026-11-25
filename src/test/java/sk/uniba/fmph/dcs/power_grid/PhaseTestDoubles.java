package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Small dependency fakes for the resource-buying and house-building phase tests. */
final class PhaseTestDoubles {
    private PhaseTestDoubles() {
    }

    static Map<PlayerColor, PlayerBoardPort> players(final PlayerColor... colors) {
        Map<PlayerColor, PlayerBoardPort> players = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color : colors) {
            players.put(color, new Board());
        }
        return players;
    }

    static final class Board implements PlayerBoardPort {
        int money = 50;
        int resourceCalls;
        int spendCalls;
        List<Resource> boughtResources = List.of();
        Optional<RequestError> resourceError = Optional.empty();

        @Override
        public Optional<RequestError> buyResources(final List<Resource> resources) {
            resourceCalls++;
            boughtResources = List.copyOf(resources);
            return resourceError;
        }

        @Override
        public int outstandingMoney() {
            return money;
        }

        @Override
        public void spendMoney(final int amount) {
            spendCalls++;
            money -= amount;
        }

        @Override
        public PowerPlantNumber bestPowerPlantNumber() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int numberOfConnectableCities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasEnoughMoney(final int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean receivePowerPlant(final PowerPlant plant, final int price) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RequestError> removePowerPlant(final PowerPlantNumber number,
                final List<Resource> removedResources) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Variant<Integer, RequestError> usePowerPlants(final List<PowerPlantNumber> numbers,
                final List<Resource> resourcesToBurn) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void receiveMoney(final int numberOfTowns) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String state() {
            throw new UnsupportedOperationException();
        }
    }

    static final class Towns implements TownsPort {
        final Map<PlayerColor, Integer> houses = new EnumMap<>(PlayerColor.class);
        int cost = 12;
        int buyCalls;
        int maxHousesCalls;
        int receivedMaxMoney;
        PlayerColor receivedPlayer;
        List<String> receivedTownNames = List.of();
        RequestError error;

        @Override
        public Variant<Integer, RequestError> buyHouses(final PlayerColor player,
                final List<String> townNames, final int maxMoney) {
            buyCalls++;
            receivedPlayer = player;
            receivedTownNames = List.copyOf(townNames);
            receivedMaxMoney = maxMoney;
            if (error != null) {
                return Variant.failure(error);
            }
            houses.merge(player, townNames.size(), Integer::sum);
            return Variant.success(cost);
        }

        @Override
        public int getNumberOfHouses(final PlayerColor player) {
            return houses.getOrDefault(player, 0);
        }

        @Override
        public int getMaxNumberOfHouses() {
            maxHousesCalls++;
            return houses.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        @Override
        public void startStep2() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep3() {
            throw new UnsupportedOperationException();
        }
    }

    static final class Market implements PowerPlantMarketPort {
        final List<Integer> cleanupLimits = new ArrayList<>();

        @Override
        public void removePowerPlantsCheaperThan(final int value) {
            cleanupLimits.add(value);
        }

        @Override
        public List<PowerPlant> currentMarket() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PowerPlant> futureMarket() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startAuction() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RequestError> canTakePowerPlant(final PowerPlantNumber number, final int price) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PowerPlant takePowerPlant(final PowerPlantNumber number) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeDiscountedPowerPlant() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void prepareNextTurn() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isStep3Start() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep2() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startStep3() {
            throw new UnsupportedOperationException();
        }
    }
}
