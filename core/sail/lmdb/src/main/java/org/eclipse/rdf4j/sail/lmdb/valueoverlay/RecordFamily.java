/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

/** Hints affect placement and candidate evaluation only; exact record bytes remain authoritative. */
public enum RecordFamily {
    IRI, TEXT, LANGUAGE, INTEGER, DECIMAL_FLOAT, TEMPORAL, BOOLEAN, BINARY, BNODE, NAMESPACE, OTHER;
    boolean trainTokens() { return this == TEXT || this == LANGUAGE || this == IRI || this == OTHER; }
}
