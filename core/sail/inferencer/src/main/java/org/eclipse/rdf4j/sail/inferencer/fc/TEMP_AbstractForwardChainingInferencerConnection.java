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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TEMP_AbstractForwardChainingInferencerConnection extends InferencerConnectionWrapper
	implements SailConnectionListener
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
	 * true if the base Sail reported added statements.
	 */
	private boolean statementsAdded;


	/*--------------*
	* Constructors *
	*--------------*/

	public TEMP_AbstractForwardChainingInferencerConnection(Sail sail, InferencerConnection con) {
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
		statementsAdded = true;
	}

	// Called by base sail
	@Override
	public void statementRemoved(Statement st) {
		statementsRemoved = true;
	}

	@Override
	public void flushUpdates()
		throws SailException
	{
		if (statementsRemoved) {
			logger.debug("full recomputation needed, starting inferencing from scratch");
			clearInferred();
			super.flushUpdates();

			addAxiomStatements();
			super.flushUpdates();
			doInferencing();
			super.flushUpdates();
		}
		else if (statementsAdded) {
			super.flushUpdates();
			doInferencing();
		}
		else {
			super.flushUpdates();
		}

		statementsAdded = false;
		statementsRemoved = false;
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
			throw new UnknownSailTransactionStateException(
				"Isolation level " + level + " not compatible with this Sail");
		}
		super.begin(compatibleLevel);
	}

	@Override
	public void rollback()
		throws SailException
	{
		super.rollback();

		statementsRemoved = false;
		statementsAdded = false;
	}

	/**
	 * Adds all basic set of axiom statements from which the complete set can be inferred to the underlying
	 * Sail.
	 */
	protected abstract void addAxiomStatements()
		throws SailException;

	abstract protected void doInferencing()
		throws SailException;

}