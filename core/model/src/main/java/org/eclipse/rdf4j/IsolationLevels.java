/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SESAME;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of Transaction isolation levels supported by RDF4J. Note that RDF4J stores are not required to support
 * all levels, consult the documentation for the specific SAIL implementation you are using to find out which levels are
 * supported.
 *
 * @author Jeen Broekstra
 * @author James Leigh
 */
public enum IsolationLevels {

	/**
	 * None: the lowest isolation level; transactions can see their own changes, but may not be able to roll them back
	 * and no support for isolation among transactions is guaranteed
	 */
	NONE,

	/**
	 * Read Uncommitted: transactions can be rolled back, but not necessarily isolated: concurrent transactions might
	 * see each other's uncommitted data (so-called 'dirty reads')
	 */
	READ_UNCOMMITTED(NONE),

	/**
	 * Read Committed: in this isolation level only statements from other transactions that have been committed (at some
	 * point) can be seen by this transaction.
	 */
	READ_COMMITTED(READ_UNCOMMITTED, NONE),

	/**
	 * Snapshot Read: in addition to {@link #READ_COMMITTED}, query results in this isolation level that are observed
	 * within a successful transaction will observe a consistent snapshot. Changes to the data occurring while a query
	 * is evaluated will not affect that query result.
	 */
	SNAPSHOT_READ(READ_COMMITTED, READ_UNCOMMITTED, NONE),

	/**
	 * Snapshot: in addition to {@link #SNAPSHOT_READ}, successful transactions in this isolation level will operate
	 * against a particular dataset snapshot. Transactions in this isolation level will see either the complete effects
	 * of other transactions (consistently throughout) or not at all.
	 */
	SNAPSHOT(SNAPSHOT_READ, READ_COMMITTED, READ_UNCOMMITTED, NONE),

	/**
	 * Serializable: in addition to {@link #SNAPSHOT}, this isolation level requires that all other successful
	 * transactions must appear to occur either completely before or completely after a successful serializable
	 * transaction.
	 */
	SERIALIZABLE(SNAPSHOT, SNAPSHOT_READ, READ_COMMITTED, READ_UNCOMMITTED, NONE);

	private final List<? extends IsolationLevels> compatibleLevels;

	private IsolationLevels(IsolationLevels... compatibleLevels) {
		this.compatibleLevels = Arrays.asList(compatibleLevels);
	}

	public boolean isCompatibleWith(IsolationLevels otherLevel) {
		return this.equals(otherLevel) || compatibleLevels.contains(otherLevel);
	}

	/**
	 * Determines the first compatible isolation level in the list of supported levels, for the given level. Returns the
	 * level itself if it is in the list of supported levels. Returns null if no compatible level can be found.
	 *
	 * @param level           the {@link IsolationLevels} for which to determine a compatible level.
	 * @param supportedLevels a list of supported isolation levels from which to select the closest compatible level.
	 * @return the given level if it occurs in the list of supported levels. Otherwise, the first compatible level in
	 *         the list of supported isolation levels, or <code>null</code> if no compatible level can be found.
	 * @throws IllegalArgumentException if either one of the input parameters is <code>null</code>.
	 */
	public static IsolationLevels getCompatibleIsolationLevel(IsolationLevels level,
			List<? extends IsolationLevels> supportedLevels) {
		if (supportedLevels == null) {
			throw new IllegalArgumentException("list of supported levels may not be null");
		}
		if (level == null) {
			throw new IllegalArgumentException("level may not be null");
		}
		if (!supportedLevels.contains(level)) {
			IsolationLevels compatibleLevel = null;
			// see we if we can find a compatible level that is supported
			for (IsolationLevels supportedLevel : supportedLevels) {
				if (supportedLevel.isCompatibleWith(level)) {
					compatibleLevel = supportedLevel;
					break;
				}
			}

			return compatibleLevel;
		} else {
			return level;
		}
	}

	public IRI getURI() {
		final ValueFactory f = SimpleValueFactory.getInstance();
		return f.createIRI(SESAME.NAMESPACE, this.name());
	}
}
