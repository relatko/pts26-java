package sk.uniba.fmph.dcs.power_grid;

public record PowerPlantNumber(int number) {
    public PowerPlantNumber {
        if (number < 1) {
            throw new IllegalArgumentException("A power plant number must be positive.");
        }
    }
}
