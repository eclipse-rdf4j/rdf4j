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
 * Service provider that hands out {@link KindAwareVar} instances for tests.
 */
public class KindAwareVarProvider implements Var.Provider {

	@Override
	public Var newVar(String name, Value value, boolean anonymous, boolean constant) {
		return new KindAwareVar(name, value, anonymous, constant);
	}

	@Override
	public Var cloneVar(Var original) {
		KindAwareVar source = (KindAwareVar) original;
		KindAwareVar clone = new KindAwareVar(source.getName(), source.getValue(), source.isAnonymous(),
				source.isConstant());
		clone.setKind(source.getKind());
		return clone;
	}
}
