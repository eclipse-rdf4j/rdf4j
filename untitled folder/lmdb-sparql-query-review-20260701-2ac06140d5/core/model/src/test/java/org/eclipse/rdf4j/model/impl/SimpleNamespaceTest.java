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

package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.NamespaceTest;

public class SimpleNamespaceTest extends NamespaceTest {

	@Override
	protected Namespace namespace(String prefix, String name) {
		return new SimpleNamespace(prefix, name);
	}

}
