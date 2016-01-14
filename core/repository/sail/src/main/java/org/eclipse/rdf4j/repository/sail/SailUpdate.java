/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.impl.AbstractOperation;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.helpers.SailUpdateExecutor;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeen Broekstra
 */
public class SailUpdate extends AbstractOperation implements Update {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final ParsedUpdate parsedUpdate;

	private final SailRepositoryConnection con;

	protected SailUpdate(ParsedUpdate parsedUpdate, SailRepositoryConnection con) {
		this.parsedUpdate = parsedUpdate;
		this.con = con;
	}

	public ParsedUpdate getParsedUpdate() {
		return parsedUpdate;
	}

	protected SailRepositoryConnection getConnection() {
		return con;
	}

	@Override
	public String toString() {
		return parsedUpdate.toString();
	}

	public void execute()
		throws UpdateExecutionException
	{
		List<UpdateExpr> updateExprs = parsedUpdate.getUpdateExprs();
		Map<UpdateExpr, Dataset> datasetMapping = parsedUpdate.getDatasetMapping();

		ValueFactory vf = getConnection().getValueFactory();
		SailConnection conn = getConnection().getSailConnection();
		SailUpdateExecutor executor = new SailUpdateExecutor(conn, vf, getConnection().getParserConfig());

		for (UpdateExpr updateExpr : updateExprs) {

			Dataset activeDataset = getMergedDataset(datasetMapping.get(updateExpr));

			try {
				boolean localTransaction = !getConnection().isActive();
				if (localTransaction) {
					getConnection().begin();
				}

				executor.executeUpdate(updateExpr, activeDataset, getBindings(), true, getMaxExecutionTime());

				if (localTransaction) {
					getConnection().commit();
				}
			}
			catch (SailException e) {
				logger.warn("exception during update execution: ", e);
				if (!updateExpr.isSilent()) {
					throw new UpdateExecutionException(e);
				}
			}
			catch (RepositoryException e) {
				logger.warn("exception during update execution: ", e);
				if (!updateExpr.isSilent()) {
					throw new UpdateExecutionException(e);
				}
			}
			catch (RDFParseException e) {
				logger.warn("exception during update execution: ", e);
				if (!updateExpr.isSilent()) {
					throw new UpdateExecutionException(e);
				}
			}
			catch (IOException e) {
				logger.warn("exception during update execution: ", e);
				if (!updateExpr.isSilent()) {
					throw new UpdateExecutionException(e);
				}
			}
		}
	}

	protected Dataset getMergedDataset(Dataset sparqlDefinedDataset) {
		if (sparqlDefinedDataset == null) {
			return dataset;
		}
		else if (dataset == null) {
			return sparqlDefinedDataset;
		}
		else {
			SimpleDataset mergedDataset = new SimpleDataset();

			boolean merge = false;

			Set<IRI> dgs = sparqlDefinedDataset.getDefaultGraphs();
			if (dgs != null && dgs.size() > 0) {
				merge = true;
				// one or more USING-clauses in the update itself, we need to define
				// the default graphs by means of the update itself
				for (IRI graphURI : dgs) {
					mergedDataset.addDefaultGraph(graphURI);
				}
			}

			Set<IRI> ngs = sparqlDefinedDataset.getNamedGraphs();
			if (ngs != null && ngs.size() > 0) {
				merge = true;
				// one or more USING NAMED-claused in the update, we need to define
				// the named graphs by means of the update itself.
				for (IRI graphURI : ngs) {
					mergedDataset.addNamedGraph(graphURI);
				}
			}

			if (merge) {
				mergedDataset.setDefaultInsertGraph(dataset.getDefaultInsertGraph());

				for (IRI graphURI : dataset.getDefaultRemoveGraphs()) {
					mergedDataset.addDefaultRemoveGraph(graphURI);
				}

				return mergedDataset;
			}
			else {
				return sparqlDefinedDataset;
			}

		}
	}
}
