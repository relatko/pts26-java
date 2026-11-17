package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PowerPlantNumberTest {

    @Test
    public void testValueAndToString() {
        PowerPlantNumber pp = new PowerPlantNumber(13);
        assertEquals(13, pp.getValue());
        assertEquals("13", pp.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        PowerPlantNumber pp1 = new PowerPlantNumber(20);
        PowerPlantNumber pp2 = new PowerPlantNumber(20);
        PowerPlantNumber pp3 = new PowerPlantNumber(25);

        assertEquals(pp1, pp1);
        assertEquals(pp1, pp2);
        assertNotEquals(pp1, pp3);
        assertNotEquals(pp1, null);
        assertNotEquals(pp1, "20");

        assertEquals(pp1.hashCode(), pp2.hashCode());
    }

    @Test
    public void testCompareTo() {
        PowerPlantNumber small = new PowerPlantNumber(5);
        PowerPlantNumber large = new PowerPlantNumber(17);
        PowerPlantNumber equalSmall = new PowerPlantNumber(5);

        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
        assertEquals(0, small.compareTo(equalSmall));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareToNull() {
        PowerPlantNumber pp = new PowerPlantNumber(10);
        pp.compareTo(null);
    }
}
