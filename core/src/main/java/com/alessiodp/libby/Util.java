package com.alessiodp.libby;

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
    public static String replaceWithDots(String str) {
        return str.replace("{}", ".");
    }

    private Util() {
        throw new UnsupportedOperationException("Util class.");
    }
}
