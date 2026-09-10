/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

public record PhysicalRecord(byte[] bytes, boolean large, int logicalLength) {
	public PhysicalRecord {
		if (bytes == null)
			throw new NullPointerException("bytes");
		if (logicalLength < 0)
			throw new IllegalArgumentException("negative logical length");
	}
}
