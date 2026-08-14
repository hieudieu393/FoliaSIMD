package org.bukkit.map;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vectorized map color matching derived from Pufferfish's SIMD implementation.
 *
 * <p>Original implementation by Kevin Raneri / Pufferfish, adapted for
 * FoliaSIMD without exposing internal palette state as public API.</p>
 */
final class FoliaSIMDMapPalette {
    private static final VectorSpecies<Integer> I_SPEC = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Float> F_SPEC = FloatVector.SPECIES_PREFERRED;

    private FoliaSIMDMapPalette() {
    }

    static boolean isSupported(Logger logger) {
        logger.log(Level.INFO, "FoliaSIMD: max SIMD vector size is {0} bits (int)", I_SPEC.vectorBitSize());
        logger.log(Level.INFO, "FoliaSIMD: max SIMD vector size is {0} bits (float)", F_SPEC.vectorBitSize());
        return I_SPEC.length() >= 2 && F_SPEC.length() >= 2;
    }

    static void matchColorVectorized(int[] in, byte[] out) {
        final int speciesLength = I_SPEC.length();
        int i;

        for (i = 0; i <= in.length - speciesLength; i += speciesLength) {
            final float[] redsArray = new float[speciesLength];
            final float[] greensArray = new float[speciesLength];
            final float[] bluesArray = new float[speciesLength];
            final int[] alphasArray = new int[speciesLength];

            for (int lane = 0; lane < speciesLength; lane++) {
                final int argb = in[i + lane];
                alphasArray[lane] = (argb >>> 24) & 0xFF;
                redsArray[lane] = (argb >>> 16) & 0xFF;
                greensArray[lane] = (argb >>> 8) & 0xFF;
                bluesArray[lane] = argb & 0xFF;
            }

            final IntVector alphas = IntVector.fromArray(I_SPEC, alphasArray, 0);
            final FloatVector reds = FloatVector.fromArray(F_SPEC, redsArray, 0);
            final FloatVector greens = FloatVector.fromArray(F_SPEC, greensArray, 0);
            final FloatVector blues = FloatVector.fromArray(F_SPEC, bluesArray, 0);
            IntVector resultIndex = IntVector.zero(I_SPEC);
            final VectorMask<Integer> modificationMask = alphas.lt(128).not();
            FloatVector bestDistances = FloatVector.broadcast(F_SPEC, Float.MAX_VALUE);

            for (int colorIndex = 4; colorIndex < MapPalette.colors.length; colorIndex++) {
                final FloatVector comparisonReds = FloatVector.broadcast(F_SPEC, MapPalette.colors[colorIndex].getRed());
                final FloatVector comparisonGreens = FloatVector.broadcast(F_SPEC, MapPalette.colors[colorIndex].getGreen());
                final FloatVector comparisonBlues = FloatVector.broadcast(F_SPEC, MapPalette.colors[colorIndex].getBlue());

                final FloatVector redMean = reds.add(comparisonReds).div(2.0F);
                final FloatVector redDifference = reds.sub(comparisonReds);
                final FloatVector greenDifference = greens.sub(comparisonGreens);
                final FloatVector blueDifference = blues.sub(comparisonBlues);

                final FloatVector redWeight = redMean.div(256.0F).add(2.0F);
                final FloatVector greenWeight = FloatVector.broadcast(F_SPEC, 4.0F);
                final FloatVector blueWeight = FloatVector.broadcast(F_SPEC, 255.0F).sub(redMean).div(256.0F).add(2.0F);

                final FloatVector distance = redWeight.mul(redDifference).mul(redDifference)
                    .add(greenWeight.mul(greenDifference).mul(greenDifference))
                    .add(blueWeight.mul(blueDifference).mul(blueDifference));

                final VectorMask<Float> betterDistance = distance.lt(bestDistances);
                bestDistances = bestDistances.blend(distance, betterDistance);
                resultIndex = resultIndex.blend(colorIndex, betterDistance.cast(I_SPEC).and(modificationMask));
            }

            for (int lane = 0; lane < speciesLength; lane++) {
                final int index = resultIndex.lane(lane);
                out[i + lane] = (byte) (index < 128 ? index : -129 + (index - 127));
            }
        }

        for (; i < in.length; i++) {
            out[i] = MapPalette.matchColor(new Color(in[i], true));
        }
    }
}
