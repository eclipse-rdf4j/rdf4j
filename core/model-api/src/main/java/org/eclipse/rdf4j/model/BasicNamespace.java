/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import java.util.Objects;

class BasicNamespace implements Namespace {

	private static final long serialVersionUID = -5858783508210775531L;


	private final String prefix;
	private final String name;


	BasicNamespace(String prefix, String name) {
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


	@Override
	public boolean equals(final Object object) {
		return this == object || object instanceof Namespace
				&& Objects.equals(prefix, ((Namespace) object).getPrefix())
				&& Objects.equals(name, ((Namespace) object).getName());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(prefix)
				^ Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return String.format("@prefix %s: <%s> .", prefix, name);
	}

}
