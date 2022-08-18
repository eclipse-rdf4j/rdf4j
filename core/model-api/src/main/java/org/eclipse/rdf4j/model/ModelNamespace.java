/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model;

import org.eclipse.rdf4j.model.base.AbstractNamespace;

class ModelNamespace extends AbstractNamespace {

	private static final long serialVersionUID = -6677122398220985317L;

	private final String prefix;
	private final String name;

	ModelNamespace(String prefix, String name) {
		this.prefix = prefix;
		this.name = name;
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public String getName() {
		return name;
	}

}
