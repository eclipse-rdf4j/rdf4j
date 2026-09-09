/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public final class FfmAccess {
    public static final ValueLayout.OfShort SHORT_LE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfInt INT_LE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfLong LONG_LE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    private FfmAccess() {}

    public static long alignUp(long value, long alignment) {
        if (value < 0 || alignment <= 0 || (alignment & (alignment - 1)) != 0)
            throw new IllegalArgumentException("invalid alignment");
        return Math.addExact(value, alignment - 1) & -alignment;
    }
}
