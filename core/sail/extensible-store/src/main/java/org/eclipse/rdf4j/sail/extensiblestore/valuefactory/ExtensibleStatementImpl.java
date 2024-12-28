/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.valuefactory;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.GenericStatement;

public class ExtensibleStatementImpl extends GenericStatement<Resource, IRI, Value> implements ExtensibleStatement {

	private final boolean inferred;

	/**
	 * Creates a new Statement with the supplied subject, predicate and object for the specified associated context.
	 *
	 * @param subject   The statement's subject, must not be <var>null</var>.
	 * @param predicate The statement's predicate, must not be <var>null</var>.
	 * @param object    The statement's object, must not be <var>null</var>.
	 * @param context   The statement's context, <var>null</var> to indicate no context is associated.
	 */
	public ExtensibleStatementImpl(Resource subject, IRI predicate, Value object, Resource context,
			boolean inferred) {
		super(subject, predicate, object, context);
		this.inferred = inferred;
	}

	@Override
	public boolean isInferred() {
		return inferred;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Statement)) {
			return false;
		}
		if (!(o instanceof ExtensibleStatement)) {
			return super.equals(o);
		}
		if (!super.equals(o)) {
			return false;
		}
		ExtensibleStatement that = (ExtensibleStatement) o;
		return inferred == that.isInferred();
	}

}
