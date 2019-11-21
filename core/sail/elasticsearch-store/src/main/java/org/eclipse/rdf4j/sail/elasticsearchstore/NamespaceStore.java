/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;

/**
 * @Author Håvard Mikkelsen Ottestad
 */
public interface NamespaceStore extends Iterable<SimpleNamespace> {
	String getNamespace(String prefix);

	void setNamespace(String prefix, String namespace);

	void removeNamespace(String prefix);

	void clear();

	void init();
}
