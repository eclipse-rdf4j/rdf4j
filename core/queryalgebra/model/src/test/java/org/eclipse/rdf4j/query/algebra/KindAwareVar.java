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
package org.eclipse.rdf4j.query.algebra;

import org.eclipse.rdf4j.model.Value;

/**
 * Test-only Var subtype that carries an extra piece of provider-managed state.
 */
@SuppressWarnings("removal")
class KindAwareVar extends Var {

	private String kind;

	KindAwareVar(String name, Value value, boolean anonymous, boolean constant) {
		super(name, value, anonymous, constant);
	}

	String getKind() {
		return kind;
	}

	void setKind(String kind) {
		this.kind = kind;
	}
}
