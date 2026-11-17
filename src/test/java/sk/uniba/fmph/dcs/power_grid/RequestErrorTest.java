package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class RequestErrorTest {

    @Test
    public void testDescriptionAndToString() {
        RequestError error = new RequestError("Not enough Elektro");
        assertEquals("Not enough Elektro", error.getDescription());
        assertEquals("RequestError{description='Not enough Elektro'}", error.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        RequestError err1 = new RequestError("Error A");
        RequestError err2 = new RequestError("Error A");
        RequestError err3 = new RequestError("Error B");

        assertEquals(err1, err1);
        assertEquals(err1, err2);
        assertNotEquals(err1, err3);
        assertNotEquals(err1, null);
        assertNotEquals(err1, "Error A");

        assertEquals(err1.hashCode(), err2.hashCode());
    }

    @Test(expected = NullPointerException.class)
    public void testNullDescription() {
        new RequestError(null);
    }
}
