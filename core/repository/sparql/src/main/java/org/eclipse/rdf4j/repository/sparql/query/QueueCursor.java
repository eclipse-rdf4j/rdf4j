/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.query;


/**
 * Makes working with a queue easier by adding the methods {@link #done()} and
 * {@link #toss(Exception)} and automatically converting the exception into a
 * QueryEvaluationException with an appropriate stack trace.
 * 
 * @author James Leigh
 * @deprecated use {@link org.eclipse.rdf4j.http.client.QueueCursor} instead
 * @see org.eclipse.rdf4j.http.client.QueueCursor
 */
public class QueueCursor<E> extends org.eclipse.rdf4j.http.client.QueueCursor<E> {

	public QueueCursor(int capacity, boolean fair) {
		super(capacity, fair);
	}

	public QueueCursor(int capacity) {
		super(capacity);
	}	
}
