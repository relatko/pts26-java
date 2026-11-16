package sk.uniba.fmph.dcs.power_grid;

import java.util.List;

/**
 * Interface for ResourcePool as seen by PlayerResources.
 */
public interface InterfaceResourcePoolPlayerResources {
    /**
     * Returns consumed or discarded resource tokens back to the supply pool.
     *
     * @param resources list of resource tokens to return
     */
    void putResources(List<Resource> resources);
}
