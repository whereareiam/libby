package com.alessiodp.libby;

import org.jetbrains.annotations.NotNull;

/**
 * Libby's utility class.
 */
public final class Util {

    /**
     * Replaces the "{}" inside the provided string with a dot.
     *
     * @param str The string to replace
     * @return The string with "{}" replaced
     */
    @NotNull
    public static String replaceWithDots(@NotNull String str) {
        return str.replace("{}", ".");
    }

    private Util() {
        throw new UnsupportedOperationException("Util class.");
    }
}
