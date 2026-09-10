/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

/** A cache build refused its native reservation. The authoritative store is unchanged. */
public final class OverlayCapacityException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public OverlayCapacityException(String message) {
		super(message);
	}
}
