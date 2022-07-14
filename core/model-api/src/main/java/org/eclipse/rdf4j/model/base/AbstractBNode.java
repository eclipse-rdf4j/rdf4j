/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.BNode;

/**
 * Base class for {@link BNode}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractBNode implements BNode {

	private static final long serialVersionUID = -437354568418943981L;

	@Override
	public String stringValue() {
		return getID();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof BNode
				&& getID().equals(((BNode) o).getID());
	}

	@Override
	public int hashCode() {
		return getID().hashCode();
	}

	@Override
	public String toString() {
		return "_:" + getID();
	}

	static class GenericBNode extends AbstractBNode {

		private static final long serialVersionUID = -617790782100827067L;

		private final String id;

		GenericBNode(String id) {
			this.id = id;
		}

		@Override
		public String getID() {
			return id;
		}

	}

}
