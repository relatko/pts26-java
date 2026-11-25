package sk.uniba.fmph.dcs.power_grid;

import java.util.List;

public interface TownsPort {
    Variant<Integer, RequestError> buyHouses(PlayerColor player, List<String> townNames, int maxMoney);
    int getNumberOfHouses(PlayerColor player);
    int getMaxNumberOfHouses();
    void startStep2();
    void startStep3();
}
