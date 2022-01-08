/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

/**
 * A Transaction Isolation Level. Default isolation levels supported by RDF4J are provided by constants {@link #NONE},
 * {@link #READ_UNCOMMITTED}, {@link #READ_COMMITTED}, {@link #SNAPSHOT_READ}, {@link #SNAPSHOT} and
 * {@link #SERIALIZABLE}.
 * 
 * Third-party store implementors may choose to add additional IsolationLevel implementations if their triplestore's
 * isolation contract is different from what is provided by default.
 *
 * @author Jeen Broekstra
 */
public interface IsolationLevel extends TransactionSetting {

	/**
	 * Shared constant for the {@link TransactionSetting} name used for isolation levels.
	 */
	String NAME = IsolationLevel.class.getCanonicalName();

	/**
	 * Verifies if this transaction isolation level is compatible with the supplied other isolation level - that is, if
	 * this transaction isolation level offers at least the same guarantees as the other level. By definition, every
	 * transaction isolation level is compatible with itself.
	 *
	 * @param otherLevel an other isolation level to check compatibility against.
	 * @return true iff this isolation level is compatible with the supplied other isolation level, false otherwise.
	 */
	boolean isCompatibleWith(IsolationLevel otherLevel);

	@Override
	default String getName() {
		return NAME;
	}

	@Override
	default String getValue() {
		return this.toString();
	}

	/**
	 * None: the lowest isolation level; transactions can see their own changes, but may not be able to roll them back
	 * and no support for isolation among transactions is guaranteed
	 */
	@SuppressWarnings("removal")
	final IsolationLevel NONE = new DefaultIsolationLevel(IsolationLevels.NONE);

	/**
	 * Read Uncommitted: transactions can be rolled back, but not necessarily isolated: concurrent transactions might
	 * see each other's uncommitted data (so-called 'dirty reads')
	 */
	@SuppressWarnings("removal")
	final IsolationLevel READ_UNCOMMITTED = new DefaultIsolationLevel(NONE, IsolationLevels.READ_UNCOMMITTED,
			IsolationLevels.NONE);

	/**
	 * Read Committed: in this isolation level only statements from other transactions that have been committed (at some
	 * point) can be seen by this transaction.
	 */
	@SuppressWarnings("removal")
	final IsolationLevel READ_COMMITTED = new DefaultIsolationLevel(READ_UNCOMMITTED, NONE,
			IsolationLevels.READ_COMMITTED, IsolationLevels.READ_UNCOMMITTED, IsolationLevels.NONE);

	/**
	 * Snapshot Read: in addition to {@link #READ_COMMITTED}, query results in this isolation level that are observed
	 * within a successful transaction will observe a consistent snapshot. Changes to the data occurring while a query
	 * is evaluated will not affect that query result.
	 */
	@SuppressWarnings("removal")
	final IsolationLevel SNAPSHOT_READ = new DefaultIsolationLevel(READ_COMMITTED, READ_UNCOMMITTED, NONE,
			IsolationLevels.SNAPSHOT_READ, IsolationLevels.READ_COMMITTED, IsolationLevels.READ_UNCOMMITTED,
			IsolationLevels.NONE);

	/**
	 * Snapshot: in addition to {@link #SNAPSHOT_READ}, successful transactions in this isolation level will operate
	 * against a particular dataset snapshot. Transactions in this isolation level will see either the complete effects
	 * of other transactions (consistently throughout) or not at all.
	 */
	@SuppressWarnings({ "removal" })
	final IsolationLevel SNAPSHOT = new DefaultIsolationLevel(SNAPSHOT_READ, READ_COMMITTED, READ_UNCOMMITTED,
			NONE,
			IsolationLevels.SNAPSHOT, IsolationLevels.SNAPSHOT_READ, IsolationLevels.READ_COMMITTED,
			IsolationLevels.READ_UNCOMMITTED, IsolationLevels.NONE);

	/**
	 * Serializable: in addition to {@link #SNAPSHOT}, this isolation level requires that all other successful
	 * transactions must appear to occur either completely before or completely after a successful serializable
	 * transaction.
	 */
	@SuppressWarnings({ "removal" })
	final IsolationLevel SERIALIZABLE = new DefaultIsolationLevel(SNAPSHOT, SNAPSHOT_READ, READ_COMMITTED,
			READ_UNCOMMITTED, NONE, IsolationLevels.SERIALIZABLE, IsolationLevels.SNAPSHOT,
			IsolationLevels.SNAPSHOT_READ, IsolationLevels.READ_COMMITTED, IsolationLevels.READ_UNCOMMITTED,
			IsolationLevels.NONE);

}
