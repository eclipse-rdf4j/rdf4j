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
import org.eclipse.rdf4j.model.Value;

public abstract class AbstractIRI implements IRI {

	private static final long serialVersionUID=7799969821538513046L;

	@Override
	public String stringValue() {
		return getNamespace()+getLocalName();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof IRI
				&& Objects.equals(stringValue(), ((Value)o).stringValue());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(stringValue());
	}

	@Override
	public String toString() {
		return stringValue();
	}

}
