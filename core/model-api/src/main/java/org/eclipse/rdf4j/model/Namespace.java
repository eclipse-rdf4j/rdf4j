/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A namespace, consisting of a namespace name and a prefix that has been assigned to it.
 */
public interface Namespace extends Serializable, Comparable<Namespace> {

	/**
	 * The default comparator for namespaces.
	 *
	 * <p>Sorts namespaces first by {@linkplain #getPrefix() prefix} and then by {@linkplain #getName()} () name};
	 * {@code null} values are sorted before other values.</p>
	 */
	static final Comparator<Namespace> COMPARATOR=Comparator.nullsFirst(
			Comparator.comparing(Namespace::getPrefix).thenComparing(Namespace::getName)
	);


	/**
	 * Gets the prefix of the current namespace. The default namespace is represented by an empty prefix string.
	 *
	 * @return prefix of namespace, or an empty string in case of the default namespace.
	 */
	public String getPrefix();

	/**
	 * Gets the name of the current namespace (i.e. its IRI).
	 *
	 * @return name of namespace
	 */
	public String getName();


	/**
	 * @inheritDoc
	 *
	 * <p>The default implementation compares this namespace with the reference namespace using the
	 * {@linkplain #COMPARATOR default namespace comparator}.</p>
	 */
	@Override
	default int compareTo(Namespace o) {
		return COMPARATOR.compare(this, o);
	}

}
