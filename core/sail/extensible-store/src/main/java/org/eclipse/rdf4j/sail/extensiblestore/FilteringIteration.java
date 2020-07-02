/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

/**
 * A wrapper for an Iteration that filters the statements against a pattern similar to getStatements(Resource subject,
 * IRI predicate, Value object, Resource... context).
 */
@Experimental
public class FilteringIteration<E extends ExtensibleStatement, X extends Exception> extends LookAheadIteration<E, X> {

	final Iteration<E, X> wrappedIteration;
	CloseableIteration<E, X> closeableWrappedIteration;

	private final Resource subject;
	private final IRI predicate;
	private final Value object;
	private final boolean inferred;
	private final Resource[] context;

	public FilteringIteration(CloseableIteration<E, X> wrappedIteration, Resource subject, IRI predicate, Value object,
			boolean inferred, Resource... context) {
		this.wrappedIteration = wrappedIteration;
		this.closeableWrappedIteration = wrappedIteration;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.inferred = inferred;
		this.context = context;
	}

	public FilteringIteration(Iteration<E, X> wrappedIteration, Resource subject, IRI predicate, Value object,
			boolean inferred, Resource... context) {
		this.wrappedIteration = wrappedIteration;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.inferred = inferred;
		this.context = context;
	}

	@Override
	protected E getNextElement() throws X {

		while (wrappedIteration.hasNext()) {
			E next = wrappedIteration.next();

			if (subject != null && !next.getSubject().equals(subject)) {
				continue;
			}
			if (predicate != null && !next.getPredicate().equals(predicate)) {
				continue;
			}
			if (object != null && !next.getObject().equals(object)) {
				continue;
			}
			if (context != null && context.length > 0
					&& !containsContext(context, next.getContext())) {
				continue;
			}

			if (next.isInferred() != inferred) {
				continue;
			}

			return next;

		}

		return null;
	}

	@Override
	protected void handleClose() throws X {
		super.handleClose();
		if (closeableWrappedIteration != null) {
			assert (wrappedIteration == closeableWrappedIteration);
			closeableWrappedIteration.close();
		}
	}

	private static boolean containsContext(Resource[] haystack, Resource needle) {
		for (Resource resource : haystack) {
			if (resource == null && needle == null) {
				return true;
			}
			if (resource != null && resource.equals(needle)) {
				return true;
			}
		}
		return false;
	}
}
