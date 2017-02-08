/**
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.inferencer.lucene;

import java.io.IOException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.lucene.LuceneIndex;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SailConnection} with support of {@link LuceneIndex}.
 * 
 * @author github.com/jgrzebyta
 */
public class LuceneSpinSailConnection extends NotifyingSailConnectionWrapper {

	private SearchIndex idx;

	private ValueFactory vf;

	private static final Logger LOG = LoggerFactory.getLogger(LuceneSpinSailConnection.class);

	public LuceneSpinSailConnection(NotifyingSailConnection wrappedCon, ValueFactory vf, SearchIndex si) {
		super(wrappedCon);
		this.vf = vf;
		this.idx = si;
	}

	@Override
	public void clear(Resource... contexts)
		throws SailException
	{
		super.clear(contexts);
		try {
			idx.clearContexts(contexts);
		}
		catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		super.removeStatements(subj, pred, obj, contexts);
		for (Resource graph : contexts) {
			Statement st = vf.createStatement(subj, pred, obj, graph);
			try {
				idx.removeStatement(st);
			}
			catch (IOException e) {
				LOG.error("Error during processing statement: {}", st.toString());
				throw new SailException(e);
			}
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		super.addStatement(subj, pred, obj, contexts);
		for (Resource graph : contexts) {
			Statement st = vf.createStatement(subj, pred, obj, graph);
			try {
				idx.addStatement(st);
			}
			catch (IOException e) {
				LOG.error("Error during processing statement: {}", st.toString());
				throw new SailException(e);
			}
		}
	}

	@Override
	public void begin()
		throws SailException
	{
		super.begin();
		try {
			idx.begin();
		}
		catch (IOException e) {
			throw new SailException(e);
		}

	}

	@Override
	public void close()
		throws SailException
	{
		super.close();
		try {
			idx.endReading();
		}
		catch (IOException e) {
			LOG.warn("LuceneIndex or SearchIndex is not closed properly");
		}
	}

	@Override
	public void commit()
		throws SailException
	{
		super.commit();
		try {
			idx.commit();
		}
		catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void rollback()
		throws SailException
	{
		super.rollback();
		try {
			idx.rollback();
		}
		catch (IOException e) {
			throw new SailException(e);
		}
	}

}
