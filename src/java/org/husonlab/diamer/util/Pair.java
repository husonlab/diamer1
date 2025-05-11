package org.husonlab.diamer.util;

import org.jetbrains.annotations.Nullable;

/**
 * A simple pair record.
 * @param <S> The type of the first element.
 * @param <T> The type of the second element.
 */
public record Pair<S, T>(@Nullable S first, @Nullable T last) {
    @Override
    public String toString() {
        return this.first + ", " + this.last;
    }
}