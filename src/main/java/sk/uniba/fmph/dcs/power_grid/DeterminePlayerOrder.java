package sk.uniba.fmph.dcs.power_grid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.Comparator;
public final class DeterminePlayerOrder {
    private final TownsPort towns;
    private final Map<PlayerColor, PlayerBoardPort> players;
    public DeterminePlayerOrder(final TownsPort towns, final Map<PlayerColor, PlayerBoardPort> players) {
        this.towns = towns;
        this.players = new EnumMap<>(players);
    }
    public List<PlayerColor> initialOrder(final Random random) {
        List<PlayerColor> order = new ArrayList<>(players.keySet());
        Collections.shuffle(order, random);
        return order;
    }
    public List<PlayerColor> reorder() {
        return players.keySet().stream()
                .sorted(Comparator
                        .comparingInt((PlayerColor c) -> towns.getNumberOfHouses(c)).reversed()
                        .thenComparing(c -> players.get(c).bestPowerPlantNumber().number(),
                                Comparator.reverseOrder()))
                .toList();
    }
    public List<PlayerColor> gameResult() {
        return players.keySet().stream()
                .sorted(Comparator
                        .comparingInt((PlayerColor c) -> poweredCities(c)).reversed()
                        .thenComparing(c -> players.get(c).outstandingMoney(),
                                Comparator.reverseOrder()))
                .toList();
    }

    private int poweredCities(final PlayerColor c) {
        return Math.min(towns.getNumberOfHouses(c),
                players.get(c).numberOfConnectableCities());
    }
}
