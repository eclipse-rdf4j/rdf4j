/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.eclipse.rdf4j.sail.model.SailModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractForwardChainingInferencerConnection extends InferencerConnectionWrapper implements
		SailConnectionListener
{

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/*-----------*
	 * Variables *
	 *-----------*/

	private Sail sail;

	/**
	 * true if the base Sail reported removed statements.
	 */
	private boolean statementsRemoved;

	/**
	 * Contains the statements that have been reported by the base Sail as
	 */
	private Model newStatements;

	protected int totalInferred;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AbstractForwardChainingInferencerConnection(Sail sail, InferencerConnection con) {
		super(con);
		this.sail = sail;
		con.addConnectionListener(this);
	}

	/*---------*
	 * Methods *
	 *---------*/

	// Called by base sail
	@Override
	public void statementAdded(Statement st) {
		if (statementsRemoved) {
			// No need to record, starting from scratch anyway
			return;
		}

		if (newStatements == null) {
			newStatements = createModel();
		}
		newStatements.add(st);
	}

	protected abstract Model createModel();

	// Called by base sail
	@Override
	public void statementRemoved(Statement st) {
		statementsRemoved = true;
		newStatements = null;
	}

	@Override
	public void flushUpdates()
		throws SailException
	{
		super.flushUpdates();

		if (statementsRemoved) {
			logger.debug("statements removed, starting inferencing from scratch");
			clearInferred();
			addAxiomStatements();

			newStatements = new SailModel(getWrappedConnection(), true);

			statementsRemoved = false;
		}

		if(hasNewStatements()) {
			doInferencing();
		}

		newStatements = null;
	}

	@Override
	public void begin()
		throws SailException
	{
		this.begin(null);
	}

	@Override
	public void begin(IsolationLevel level)
		throws SailException
	{
		if (level == null) {
			level = sail.getDefaultIsolationLevel();
		}

		IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(level,
				sail.getSupportedIsolationLevels());
		if (compatibleLevel == null) {
			throw new UnknownSailTransactionStateException("Isolation level " + level
					+ " not compatible with this Sail");
		}
		super.begin(compatibleLevel);
	}

	@Override
	public void rollback()
		throws SailException
	{
		super.rollback();

		statementsRemoved = false;
		newStatements = null;
	}

	/**
	 * Adds all basic set of axiom statements from which the complete set can be
	 * inferred to the underlying Sail.
	 */
	protected abstract void addAxiomStatements() throws SailException;

	protected void doInferencing()
		throws SailException
	{
		// initialize some vars
		totalInferred = 0;
		int iteration = 0;

		while (hasNewStatements()) {
			iteration++;
			logger.debug("starting iteration " + iteration);
			Model newThisIteration = prepareIteration();

			int nofInferred = applyRules(newThisIteration);

			logger.debug("iteration " + iteration + " done; inferred " + nofInferred + " new statements");
			totalInferred += nofInferred;
		}
	}

	protected abstract int applyRules(Model iteration) throws SailException;

	protected Model prepareIteration() {
		Model newThisIteration = newStatements;
		newStatements = null;
		return newThisIteration;
	}

	protected boolean hasNewStatements() {
		return newStatements != null && !newStatements.isEmpty();
	}
}
