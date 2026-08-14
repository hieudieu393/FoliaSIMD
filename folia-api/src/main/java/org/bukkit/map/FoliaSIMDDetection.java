package org.bukkit.map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runtime detection for the Pufferfish-derived SIMD map palette optimization.
 * This class is an internal FoliaSIMD implementation detail.
 */
final class FoliaSIMDDetection {
    private static final Logger LOGGER = Logger.getLogger("FoliaSIMD");
    private static final int MIN_SUPPORTED_JAVA = 17;
    private static final int MAX_SUPPORTED_JAVA = 25;
    private static final boolean ENABLED = detect();

    private FoliaSIMDDetection() {
    }

    static boolean isEnabled() {
        return ENABLED;
    }

    private static boolean detect() {
        final int javaVersion = Runtime.version().feature();
        if (javaVersion < MIN_SUPPORTED_JAVA || javaVersion > MAX_SUPPORTED_JAVA) {
            LOGGER.log(Level.WARNING, "FoliaSIMD: SIMD map rendering is disabled on Java {0}; supported Vector API range is Java {1}-{2}.",
                new Object[] {javaVersion, MIN_SUPPORTED_JAVA, MAX_SUPPORTED_JAVA});
            return false;
        }

        try {
            if (FoliaSIMDMapPalette.isSupported(LOGGER)) {
                LOGGER.info("FoliaSIMD: SIMD map rendering is enabled.");
                return true;
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // jd.incubator.vector is optional at runtime; use the scalar fallback.
        }

        LOGGER.warning("FoliaSIMD: SIMD map rendering is available but not enabled.");
        LOGGER.warning("FoliaSIMD: add --add-modules=jdk.incubator.vector before -jar to enable it.");
        return false;
    }
}
