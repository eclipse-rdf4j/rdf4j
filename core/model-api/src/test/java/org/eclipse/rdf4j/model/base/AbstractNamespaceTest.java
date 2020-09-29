/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.NamespaceTest;

public final class AbstractNamespaceTest extends NamespaceTest {

	@Override
	protected Namespace namespace(String prefix, String name) {
		return new TestNamespace(prefix, name);
	}

	private static final class TestNamespace extends AbstractNamespace {

		private static final long serialVersionUID = -6325162028110821008L;

		private final String prefix;
		private final String name;

		private TestNamespace(String prefix, String name) {
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
