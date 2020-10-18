/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.BNode;

/**
 * Base class for {@link BNode}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractBNode implements BNode {

	private static final long serialVersionUID = -437354568418943981L;

	private static final AtomicLong nodeID = new AtomicLong(ThreadLocalRandom.current().nextLong());

	/**
	 * Creates a new blank node value.
	 *
	 * @return a new generic blank node value with a system-generated label
	 */
	public static BNode createBNode() {
		return new GenericBNode("node" + Long.toString(nodeID.incrementAndGet(), Character.MAX_RADIX));
	}

	/**
	 * Creates a new blank node value.
	 *
	 * @param nodeID the identifier of the blank node
	 *
	 * @return a new generic blank node value
	 *
	 * @throws NullPointerException if {@code nodeID} is {@code null}
	 */
	public static BNode createBNode(String nodeID) {

		if (nodeID == null) {
			throw new NullPointerException("null nodeID");
		}

		return new GenericBNode(nodeID);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public String stringValue() {
		return getID();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof BNode
				&& Objects.equals(getID(), ((BNode) o).getID());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getID());
	}

	@Override
	public String toString() {
		return "_:" + getID();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class GenericBNode extends AbstractBNode {

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
