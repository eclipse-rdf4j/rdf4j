/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A wrapper around an {@link SailDataset} to specialize the behaviour of an
 * {@link SailDataset}.
 * 
 * @author James Leigh
 */
abstract class DelegatingSailDataset implements SailDataset {

	private final SailDataset delegate;

	/**
	 * Wraps an {@link SailDataset} delegating all calls to it.
	 * 
	 * @param delegate
	 */
	public DelegatingSailDataset(SailDataset delegate) {
		this.delegate = delegate;
	}

	public String toString() {
		return delegate.toString();
	}

	public void close()
		throws SailException
	{
		delegate.close();
	}

	public CloseableIteration<? extends Namespace, SailException> getNamespaces()
		throws SailException
	{
		return delegate.getNamespaces();
	}

	public String getNamespace(String prefix)
		throws SailException
	{
		return delegate.getNamespace(prefix);
	}

	public CloseableIteration<? extends Resource, SailException> getContextIDs()
		throws SailException
	{
		return delegate.getContextIDs();
	}

	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
			Value obj, Resource... contexts)
		throws SailException
	{
		return delegate.getStatements(subj, pred, obj, contexts);
	}
}
