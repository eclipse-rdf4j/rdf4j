/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.util.Optional;
import java.util.Set;

/**
 * An interface that is used to signify that something is able to provide {@link Namespace} information, in addition to
 * {@link Statement}s.
 *
 * @author Peter Ansell
 */
@FunctionalInterface
public interface NamespaceAware {

	/**
	 * Gets the set that contains the assigned namespaces.
	 *
	 * @return A {@link Set} containing the {@link Namespace} objects that are available.
	 */
	Set<Namespace> getNamespaces();

	/**
	 * Gets the namespace that is associated with the specified prefix, if any. If multiple namespaces match the given
	 * prefix, the result may not be consistent over successive calls to this method.
	 *
	 * @param prefix A namespace prefix.
	 * @return The namespace name that is associated with the specified prefix, or {@link Optional#empty()} if there is
	 *         no such namespace.
	 */
	default Optional<Namespace> getNamespace(String prefix) {
		return getNamespaces().stream().filter(t -> t.getPrefix().equals(prefix)).findAny();
	}

}
