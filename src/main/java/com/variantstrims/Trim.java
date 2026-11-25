package com.variantstrims;

import java.util.Locale;

public enum Trim {
    SENTRY(false),
    DUNE(true),
    COAST(false),
    WILD(false),
    WARD(true),
    SILENCE(true),
    EYE(true),
    VEX(false),
    TIDE(true),
    SNOUT(false),
    RIB(false),
    SPIRE(true),
    BOLT(false),
    FLOW(true),
    WAYFINDER(false),
    RAISER(false),
    SHAPER(false),
    HOST(false);

    public final boolean strong;

    Trim(boolean strong) {
        this.strong = strong;
    }

    public static Trim byId(String path) {
        if (path == null) return null;
        try {
            return valueOf(path.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
