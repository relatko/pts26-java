package sk.uniba.fmph.dcs.power_grid;

import java.util.List;

public interface InterfaceResourcePoolResourceMarket {

    boolean canTakeResourcesFromStorageSpace(List<Resource> resources);

    void getFromStorageSpace(List<Resource> resources);

    List<Resource> takeResources(List<Resource> resources);

    String getStateString();
}
