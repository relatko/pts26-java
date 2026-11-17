package sk.uniba.fmph.dcs.power_grid;

import java.util.Objects;

public final class RequestError {

    private final String description;

    public RequestError(final String description) {
        this.description = Objects.requireNonNull(description, "Description cannot be null");
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof RequestError otherError)) {
            return false;
        }
        return Objects.equals(description, otherError.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description);
    }

    @Override
    public String toString() {
        return "RequestError{description='" + description + "'}";
    }
}
