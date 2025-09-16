package org.husonlab.diamer.util;

import org.jetbrains.annotations.NotNull;

/**
 * A simple pair record where both elements are non-null.
 * @param <S> The type of the first element.
 * @param <T> The type of the second element.
 */
public record NNPair<S, T>(@NotNull S first, @NotNull T last) {
    @Override
    public String toString() {
        return this.first + ", " + this.last;
    }
}
