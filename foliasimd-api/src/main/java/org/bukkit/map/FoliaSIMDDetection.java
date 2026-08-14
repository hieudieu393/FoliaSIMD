package org.bukkit.map;

import java.util.logging.Level;
import java.util.logging.Logger;

final class FoliaSIMDDetection {
    private static final Logger LOGGER = Logger.getLogger("FoliaSIMD");
    private static final int MIN_SUPPORTED_JAVA = 17;
    private static final int MAX_SUPPORTED_JAVA = 25;
    private static final boolean ENABLED = detect();

    private FoliaSIMDDetection() {}

    static boolean isEnabled() {
        return ENABLED;
    }

    private static boolean detect() {
        final int javaVersion = Runtime.version().feature();
        if (javaVersion < MIN_SUPPORTED_JAVA || javaVersion > MAX_SUPPORTED_JAVA) {
            LOGGER.log(Level.WARNING, "FoliaSIMD: SIMD disabled on Java {0}; supported Java range is {1}-{2}.", new Object[] {javaVersion, MIN_SUPPORTED_JAVA, MAX_SUPPORTED_JAVA});
            return false;
        }
        try {
            if (FoliaSIMDMapPalette.isSupported(LOGGER)) {
                LOGGER.info("FoliaSIMD: SIMD map rendering enabled.");
                return true;
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        LOGGER.warning("FoliaSIMD: add --add-modules=jdk.incubator.vector before -jar to enable SIMD.");
        return false;
    }
}
