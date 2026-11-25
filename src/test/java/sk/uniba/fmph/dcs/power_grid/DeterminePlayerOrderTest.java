package sk.uniba.fmph.dcs.power_grid;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.*;
final class TownsFake implements TownsPort {
    private final Map<PlayerColor, Integer> houses;
    TownsFake(Map<PlayerColor, Integer> houses) {
        this.houses = Map.copyOf(houses);
    }
    TownsFake() {
        this.houses = new EnumMap<>(PlayerColor.class);
    }
    @Override
    public int getNumberOfHouses(PlayerColor player) {
        return houses.getOrDefault(player, 0);
    }
    @Override
    public int getMaxNumberOfHouses(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public void startStep2(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public void startStep3(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public Variant<Integer, RequestError> buyHouses(PlayerColor player, List<String> townNames, int maxMoney){
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

final class PlayerBoardFake implements PlayerBoardPort {
    private final PowerPlantNumber bestPlant;
    private final int money;
    private final int connectableCities;
    PlayerBoardFake(int bestPlant, int money, int connectableCities) {
        this.bestPlant = new PowerPlantNumber(bestPlant);
        this.money = money;
        this.connectableCities = connectableCities;
    }
    @Override
    public boolean hasEnoughMoney(int amount){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public boolean receivePowerPlant(PowerPlant plant, int price){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public Optional<RequestError> removePowerPlant(PowerPlantNumber number, List<Resource> removedResources){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public Optional<RequestError> buyResources(List<Resource> resources){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public Variant<Integer, RequestError> usePowerPlants(List<PowerPlantNumber> numbers, List<Resource> resourcesToBurn){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public void receiveMoney(int numberOfTowns){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public void spendMoney(int amount){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public String state(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public PowerPlantNumber bestPowerPlantNumber() {
        return bestPlant;
    }

    @Override
    public int outstandingMoney() {
        return money;
    }

    @Override
    public int numberOfConnectableCities() {
        return connectableCities;
    }
}

public class DeterminePlayerOrderTest {
    @Test
    public void testInitialOrderDeterministicWithSeed() {
        TownsPort towns = new TownsFake();
        Map<PlayerColor, PlayerBoardPort> players = new EnumMap<>(PlayerColor.class);
        for(PlayerColor color : PlayerColor.values()) {
            players.put(color, new PlayerBoardFake(1,0,0));
        }
        DeterminePlayerOrder order=new DeterminePlayerOrder(towns,players);
        List<PlayerColor> first = order.initialOrder(new Random(42));
        List<PlayerColor> second = order.initialOrder(new Random(42));

        assertEquals(first, second);
    }
    @Test
    public void testReorder() {
        TownsPort towns = new TownsFake(Map.of(
                PlayerColor.RED, 5,
                PlayerColor.BLUE, 3,
                PlayerColor.GREEN, 5
        ));
        EnumMap<PlayerColor, PlayerBoardPort> players = new EnumMap<>(PlayerColor.class);
        players.put(PlayerColor.RED, new PlayerBoardFake(30, 100, 5));
        players.put(PlayerColor.BLUE, new PlayerBoardFake(25, 80, 8));
        players.put(PlayerColor.GREEN, new PlayerBoardFake(20, 120, 7));

        DeterminePlayerOrder order = new DeterminePlayerOrder(towns, players);
        assertEquals(
                List.of(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE),
                order.reorder()
        );
    }
    @Test
    public void testGameResult() {
        TownsPort towns = new TownsFake(Map.of(
                PlayerColor.RED, 10,
                PlayerColor.BLUE, 8,
                PlayerColor.GREEN, 7
        ));
        EnumMap<PlayerColor, PlayerBoardPort> players = new EnumMap<>(PlayerColor.class);
        players.put(PlayerColor.RED, new PlayerBoardFake(30, 100, 7));
        players.put(PlayerColor.BLUE, new PlayerBoardFake(25, 80, 8));
        players.put(PlayerColor.GREEN, new PlayerBoardFake(20, 120, 7));
        DeterminePlayerOrder order = new DeterminePlayerOrder(towns, players);

        assertEquals(
                List.of(PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.RED),
                order.gameResult()
        );
    }
}
