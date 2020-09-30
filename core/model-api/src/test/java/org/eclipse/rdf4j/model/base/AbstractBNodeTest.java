/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.BNodeTest;

/**
 * Unit tests for {@link AbstractBNode}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractBNodeTest extends BNodeTest {

	@Override
	protected TestBNode bnode(final String id) {
		return new TestBNode(id);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestBNode extends AbstractBNode {

		private static final long serialVersionUID = -617790782100827067L;

		private final String id;

		TestBNode(String id) {

			if (id == null) {
				throw new NullPointerException("null id");
			}

			this.id = id;
		}

		@Override
		public String getID() {
			return id;
		}

	}

}