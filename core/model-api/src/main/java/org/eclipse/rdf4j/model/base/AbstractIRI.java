/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;

/**
 * Base class for {@link IRI}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 * 
 * @implNote Wherever feasible, in order to avoid severe performance degradation of the {@link #equals(Object)} method,
 *           concrete subclasses should override {@link #stringValue()} to provide a constant pre-computed value
 */
public abstract class AbstractIRI implements IRI {

	private static final long serialVersionUID = 7799969821538513046L;

	/**
	 * Creates a new IRI value.
	 *
	 * @param iri the string representation of the IRI
	 * 
	 * @return a new generic IRI value
	 * 
	 * @throws NullPointerException     if {@code iri} is {@code null}
	 * @throws IllegalArgumentException if {@code iri} is not an absolute IRI
	 */
	public static IRI createIRI(String iri) {

		if (iri == null) {
			throw new NullPointerException("null iri");
		}

		if (iri.indexOf(':') < 0) {
			throw new IllegalArgumentException("missing colon in absolute IRI");
		}

		return new UnaryIRI(iri);
	}

	/**
	 * Creates a new IRI value.
	 *
	 * @param namespace the namespace of the IRI (as defined in {@link IRI})
	 * @param localName the local name of the IRI (as defined in {@link IRI})
	 *
	 * @return a new generic IRI value
	 *
	 * @throws NullPointerException     if either {@code namespace} or {@code localName} is {@code null}
	 * @throws IllegalArgumentException if {@code namespace} is not an absolute IRI
	 * 
	 * @see IRI
	 */
	public static IRI createIRI(String namespace, String localName) {

		if (namespace == null) {
			throw new NullPointerException("null namespace");
		}

		if (localName == null) {
			throw new NullPointerException("null localName");
		}

		if (namespace.indexOf(':') < 0) {
			throw new IllegalArgumentException("missing colon in absolute namespace IRI");
		}

		return new BinaryIRI(namespace, localName);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public String stringValue() {
		return getNamespace() + getLocalName();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof IRI
				&& Objects.equals(toString(), o.toString()); // !!! use stringValue()
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(toString()); // !!! use stringValue()
	}

	@Override
	public String toString() {
		return stringValue();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class UnaryIRI extends AbstractIRI {

		private static final long serialVersionUID = 2209156550690548467L;

		private final String iri;

		private int split;

		UnaryIRI(String iri) {
			this.iri = iri;
		}

		@Override
		public String stringValue() {
			return iri;
		}

		@Override
		public String getNamespace() {
			return iri.substring(0, split());
		}

		@Override
		public String getLocalName() {
			return iri.substring(split());
		}

		private int split() {
			return (split > 0) ? split
					: (split = iri.indexOf('#') + 1) > 0 ? split
					: (split = iri.lastIndexOf('/') + 1) > 0 ? split
							: (split = iri.lastIndexOf(':') + 1) > 0 ? split
									: 0; // unexpected: colon presence already tested in factory method
		}

	}

	private static class BinaryIRI extends AbstractIRI {

		private static final long serialVersionUID = 5909565726259853948L;

		private final String iri;

		private final String namespace;
		private final String localname;

		BinaryIRI(String namespace, String localname) {

			// !!! ??? ;( removing .toString() causes a 2x penalty in .equals() performance on Oracle JDK 1.8/11…

			this.iri = (namespace + localname);

			this.namespace = namespace;
			this.localname = localname;
		}

		@Override
		public String stringValue() {
			return iri;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public String getLocalName() {
			return localname;
		}

	}

}
