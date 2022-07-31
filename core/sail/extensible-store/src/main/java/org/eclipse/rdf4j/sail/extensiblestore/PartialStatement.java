/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

public class PartialStatement {

	private final boolean inferred;
	Resource subject;
	IRI predicate;
	Value object;
	Resource[] context;

	public PartialStatement(Resource subject, IRI predicate, Value object, boolean inferred, Resource... context) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.context = context;
		this.inferred = inferred;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PartialStatement that = (PartialStatement) o;
		return inferred == that.inferred &&
				Objects.equals(subject, that.subject) &&
				Objects.equals(predicate, that.predicate) &&
				Objects.equals(object, that.object) &&
				Arrays.equals(context, that.context);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(subject, predicate, object, inferred);
		result = 31 * result + Arrays.hashCode(context);
		return result;
	}
}
