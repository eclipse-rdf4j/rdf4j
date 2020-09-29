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

public abstract class AbstractNamespace implements Namespace {

	/**
	 * Sorts namespaces first by {@linkplain #getPrefix() prefix} and then by {@linkplain #getName()} () name};
	 * {@code null} values are sorted before other values.
	 */
	private static final Comparator<Namespace> COMPARATOR = Comparator.nullsFirst(
			Comparator.comparing(Namespace::getPrefix).thenComparing(Namespace::getName)
	);

	@Override
	public int compareTo(Namespace o) {
		return COMPARATOR.compare(this, o);
	}

	@Override
	public boolean equals(final Object object) {
		return this == object || object instanceof Namespace
				&& Objects.equals(getPrefix(), ((Namespace) object).getPrefix())
				&& Objects.equals(getName(), ((Namespace) object).getName());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getPrefix())
				^ Objects.hashCode(getName());
	}

	@Override
	public String toString() {
		return getPrefix() + " :: " + getName();
	}

}
