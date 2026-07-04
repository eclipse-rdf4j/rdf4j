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

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.NamespaceTest;

/**
 * Unit tests for {@link AbstractNamespace}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public final class AbstractNamespaceTest extends NamespaceTest {

	@Override
	protected Namespace namespace(String prefix, String name) {

		if (prefix == null) {
			throw new NullPointerException("null prefix");
		}

		if (name == null) {
			throw new NullPointerException("null name");
		}

		return new GenericNamespace(prefix, name);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class GenericNamespace extends AbstractNamespace {

		private static final long serialVersionUID = -6325162028110821008L;
		private final String prefix;
		private final String name;

		GenericNamespace(String prefix, String name) {
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
}
