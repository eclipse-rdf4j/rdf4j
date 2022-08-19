/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.base;

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

	@Override
	public String stringValue() {
		return getNamespace() + getLocalName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o != null) {
			if (o instanceof AbstractIRI) {
				return stringValue().equals(o.toString());
			} else {
				return o.equals(this);
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return stringValue().hashCode();
	}

	@Override
	public String toString() {
		return stringValue();
	}

	static class GenericIRI extends AbstractIRI {

		private static final long serialVersionUID = 2209156550690548467L;

		private final String iri;

		private int split;

		GenericIRI(String iri) {
			this.iri = iri;
			this.split = 0;
		}

		GenericIRI(String namespace, String localName) {
			this.iri = namespace + localName;
			this.split = namespace.length();
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
			if (split > 0) {

				return split;

			} else if ((split = iri.indexOf('#') + 1) > 0) {

				return split;

			} else if ((split = iri.lastIndexOf('/') + 1) > 0) {

				return split;

			} else if ((split = iri.lastIndexOf(':') + 1) > 0) {

				return split;

			} else {

				return 0; // unexpected: colon presence already tested in factory methods

			}
		}

	}

}
