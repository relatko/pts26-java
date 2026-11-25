package sk.uniba.fmph.dcs.power_grid;

public sealed interface Variant<S, E> {
    record Success<S, E>(S value) implements Variant<S, E> {
    }

    record Failure<S, E>(E error) implements Variant<S, E> {
    }

    static <S, E> Variant<S, E> success(final S value) {
        return new Success<>(value);
    }

    static <S, E> Variant<S, E> failure(final E error) {
        return new Failure<>(error);
    }
}

