/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.impl.AbstractParserUpdate;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.repository.sail.helpers.SailUpdateExecutor;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SailConnectionUpdate extends AbstractParserUpdate {

	private static final Logger logger = LoggerFactory.getLogger(SailConnectionUpdate.class);

	private final SailConnection con;

	private final ValueFactory vf;

	private final ParserConfig parserConfig;

	public SailConnectionUpdate(ParsedUpdate parsedUpdate, SailConnection con, ValueFactory vf,
			ParserConfig parserConfig) {
		super(parsedUpdate);
		this.con = con;
		this.vf = vf;
		this.parserConfig = parserConfig;
	}

	protected SailConnection getSailConnection() {
		return con;
	}

	@Override
	public void execute() throws UpdateExecutionException {
		ParsedUpdate parsedUpdate = getParsedUpdate();
		List<UpdateExpr> updateExprs = parsedUpdate.getUpdateExprs();
		Map<UpdateExpr, Dataset> datasetMapping = parsedUpdate.getDatasetMapping();

		SailUpdateExecutor executor = new SailUpdateExecutor(con, vf, parserConfig);

		for (UpdateExpr updateExpr : updateExprs) {

			Dataset activeDataset = getMergedDataset(datasetMapping.get(updateExpr));

			try {
				boolean localTransaction = isLocalTransaction();
				if (localTransaction) {
					beginLocalTransaction();
				}

				executor.executeUpdate(updateExpr, activeDataset, getBindings(), getIncludeInferred(),
						getMaxExecutionTime());

				if (localTransaction) {
					commitLocalTransaction();
				}
			} catch (RDF4JException | IOException e) {
				logger.warn("exception during update execution: ", e);
				if (!updateExpr.isSilent()) {
					throw new UpdateExecutionException(e);
				}
			}
		}
	}

	private boolean isLocalTransaction() throws SailException {
		return !getSailConnection().isActive();
	}

	private void beginLocalTransaction() throws SailException {
		getSailConnection().begin();
	}

	private void commitLocalTransaction() throws SailException {
		getSailConnection().commit();
	}
}
