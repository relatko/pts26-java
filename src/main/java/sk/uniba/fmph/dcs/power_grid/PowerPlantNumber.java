package sk.uniba.fmph.dcs.power_grid;

import java.util.Objects;

public final class PowerPlantNumber implements Comparable<PowerPlantNumber> {

    private final int value;

    public PowerPlantNumber(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public int getNumber() {
        return value;
    }

    @Override
    public int compareTo(final PowerPlantNumber otherPowerPlantNumber) {
        Objects.requireNonNull(otherPowerPlantNumber, "PowerPlantNumber to compare cannot be null");
        return Integer.compare(this.value, otherPowerPlantNumber.value);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof PowerPlantNumber otherPowerPlantNumber)) {
            return false;
        }
        return this.value == otherPowerPlantNumber.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
