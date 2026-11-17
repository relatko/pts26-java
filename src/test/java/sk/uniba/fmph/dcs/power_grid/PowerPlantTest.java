package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import org.junit.Test;

public class PowerPlantTest {

    @Test
    public void testGettersAndToString() {
        PowerPlantNumber ppn = new PowerPlantNumber(7);
        PowerPlant plant = new PowerPlant(ppn, List.of(Resource.OIL), 3, 4);

        assertEquals(ppn, plant.getNumber());
        assertEquals(List.of(Resource.OIL), plant.getAcceptedResources());
        assertEquals(3, plant.getResourceAmount());
        assertEquals(4, plant.getNumberOfCities());
        assertEquals(7, plant.getNumber().getNumber());
        assertEquals(7, plant.getNumber().getValue());
    }

    @Test
    public void testEqualsAndHashCode() {
        PowerPlant plant1 = new PowerPlant(new PowerPlantNumber(13), List.of(Resource.COAL), 2, 1);
        PowerPlant plant2 = new PowerPlant(new PowerPlantNumber(13), List.of(Resource.COAL), 2, 1);
        PowerPlant plant3 = new PowerPlant(new PowerPlantNumber(14), List.of(Resource.GARBAGE), 2, 2);

        assertEquals(plant1, plant1);
        assertEquals(plant1, plant2);
        assertNotEquals(plant1, plant3);
        assertNotEquals(plant1, null);

        assertEquals(plant1.hashCode(), plant2.hashCode());
    }

    @Test(expected = NullPointerException.class)
    public void testNullPowerPlantNumber() {
        new PowerPlant(null, List.of(Resource.COAL), 1, 1);
    }
}
