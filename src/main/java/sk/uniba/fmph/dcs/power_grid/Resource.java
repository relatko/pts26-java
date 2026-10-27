package sk.uniba.fmph.dcs.power_grid;

public enum Resource {
    COAL(1), OIL(2), GARBAGE(3), URANIUM(4);

    private final int value;

    Resource(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
