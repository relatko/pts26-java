package sk.uniba.fmph.dcs.power_grid;

/**
 * Interface representing PlayerMoney functionality for PlayerBoardFacade.
 */
public interface InterfacePlayerMoney {

    boolean hasEnoughMoney(int amount);

    void spendMoney(int amount);

    void receiveMoney(int numberOfTowns);

    int currentMoney();
}
