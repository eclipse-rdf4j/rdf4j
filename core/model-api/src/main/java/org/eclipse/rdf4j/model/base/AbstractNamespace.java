/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.rdf4j.model.Namespace;

/**
 * Base class for {@link Namespace}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractNamespace implements Namespace {

	private static final long serialVersionUID = 1915019376191661835L;

	/**
	 * Sorts namespaces first by {@linkplain #getPrefix() prefix} and then by {@linkplain #getName()} () name};
	 * {@code null} values are sorted before other values.
	 */
	private static final Comparator<Namespace> COMPARATOR = Comparator.nullsFirst(
			Comparator.comparing(Namespace::getPrefix).thenComparing(Namespace::getName)
	);

	/**
	 * Creates a new namespace.
	 *
	 * @param prefix the prefix of the namespace
	 * @param name   the IRI of the namespace
	 *
	 * @return a new generic namespace
	 *
	 * @throws NullPointerException if either {@code prefix} or {@code name} is {@code null}
	 */
	public static Namespace createNamespace(String prefix, String name) {

		if (prefix == null) {
			throw new NullPointerException("null prefix");
		}

		if (name == null) {
			throw new NullPointerException("null name");
		}

		return new GenericNamespace(prefix, name);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public int compareTo(Namespace o) {
		return COMPARATOR.compare(this, o);
	}

	@Override
	public boolean equals(Object object) {
		return this == object || object instanceof Namespace
				&& Objects.equals(getPrefix(), ((Namespace) object).getPrefix())
				&& Objects.equals(getName(), ((Namespace) object).getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getPrefix(), getName());
	}

	@Override
	public String toString() {
		return getPrefix() + " :: " + getName();
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
