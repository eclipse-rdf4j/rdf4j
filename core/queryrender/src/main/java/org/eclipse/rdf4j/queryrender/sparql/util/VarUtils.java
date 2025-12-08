/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.util;

import java.util.Objects;

import org.eclipse.rdf4j.query.algebra.Var;

/** Shared helpers for RDF4J Var comparison and path-var recognition. */
public final class VarUtils {
	private VarUtils() {
	}

	public static final String ANON_PATH_PREFIX = "_anon_path_";
	public static final String ANON_PATH_INVERSE_PREFIX = "_anon_path_inverse_";

	/** true if both are unbound vars with equal names. */
	public static boolean sameVar(Var a, Var b) {
		if (a == null || b == null) {
			return false;
		}
		if (a.hasValue() || b.hasValue()) {
			return false;
		}
		return Objects.equals(a.getName(), b.getName());
	}

	/**
	 * True when both variables denote the same term: compares names if both are variables without value, or compares
	 * values if both are constants. Returns false when one has a value and the other does not.
	 */
	public static boolean sameVarOrValue(Var a, Var b) {
		if (a == null || b == null) {
			return false;
		}
		final boolean av = a.hasValue();
		final boolean bv = b.hasValue();
		if (av && bv) {
			return Objects.equals(a.getValue(), b.getValue());
		}
		if (!av && !bv) {
			return Objects.equals(a.getName(), b.getName());
		}
		return false;
	}

	/** True if the given var is an anonymous path bridge variable. */
	public static boolean isAnonPathVar(Var v) {
		if (v == null || v.hasValue()) {
			return false;
		}
		String n = v.getName();
		return n != null && n.startsWith(ANON_PATH_PREFIX);
	}

	/** True when the anonymous path var explicitly encodes inverse orientation. */
	public static boolean isAnonPathInverseVar(Var v) {
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith(ANON_PATH_INVERSE_PREFIX);
	}
}
