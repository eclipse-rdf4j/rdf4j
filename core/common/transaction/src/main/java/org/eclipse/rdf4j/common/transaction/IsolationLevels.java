/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of Transaction {@link IsolationLevel}s supported by RDF4J. Note that RDF4J stores are not required to
 * support all levels, consult the documentation for the specific SAIL implementation you are using to find out which
 * levels are supported.
 *
 * @author Jeen Broekstra
 * @author James Leigh
 * @deprecated since 4.0.0. Use the corresponding constants in {@link IsolationLevel} instead.
 */
@Deprecated(forRemoval = true, since = "4.0.0")
public enum IsolationLevels implements IsolationLevel {

	/**
	 * None: the lowest isolation level; transactions can see their own changes, but may not be able to roll them back
	 * and no support for isolation among transactions is guaranteed
	 * 
	 * @deprecated since 4.0.0. Use {@link IsolationLevel#NONE} instead
	 */
	NONE(IsolationLevel.NONE),

	/**
	 * Read Uncommitted: transactions can be rolled back, but not necessarily isolated: concurrent transactions might
	 * see each other's uncommitted data (so-called 'dirty reads')
	 * 
	 * @deprecated since 4.0.0. Use {@link IsolationLevel#READ_UNCOMMITTED} instead
	 */
	READ_UNCOMMITTED(NONE, IsolationLevel.READ_UNCOMMITTED, IsolationLevel.NONE),

	/**
	 * Read Committed: in this isolation level only statements from other transactions that have been committed (at some
	 * point) can be seen by this transaction.
	 * 
	 * @deprecated since 4.0.0. Use {@link IsolationLevel#READ_COMMITTED} instead
	 */
	READ_COMMITTED(READ_UNCOMMITTED, NONE, IsolationLevel.READ_COMMITTED, IsolationLevel.READ_UNCOMMITTED,
			IsolationLevel.NONE),

	/**
	 * Snapshot Read: in addition to {@link #READ_COMMITTED}, query results in this isolation level that are observed
	 * within a successful transaction will observe a consistent snapshot. Changes to the data occurring while a query
	 * is evaluated will not affect that query result.
	 * 
	 * @deprecated since 4.0.0. Use {@link IsolationLevel#SNAPSHOT_READ} instead
	 */
	SNAPSHOT_READ(READ_COMMITTED, READ_UNCOMMITTED, NONE, IsolationLevel.SNAPSHOT_READ, IsolationLevel.READ_COMMITTED,
			IsolationLevel.READ_UNCOMMITTED, IsolationLevel.NONE),

	/**
	 * Snapshot: in addition to {@link #SNAPSHOT_READ}, successful transactions in this isolation level will operate
	 * against a particular dataset snapshot. Transactions in this isolation level will see either the complete effects
	 * of other transactions (consistently throughout) or not at all.
	 * 
	 * @deprecated since 4.0.0. Use {@link IsolationLevel#SNAPSHOT} instead
	 */
	SNAPSHOT(SNAPSHOT_READ, READ_COMMITTED, READ_UNCOMMITTED, NONE, IsolationLevel.SNAPSHOT,
			IsolationLevel.SNAPSHOT_READ, IsolationLevel.READ_COMMITTED, IsolationLevel.READ_UNCOMMITTED,
			IsolationLevel.NONE),

	/**
	 * Serializable: in addition to {@link #SNAPSHOT}, this isolation level requires that all other successful
	 * transactions must appear to occur either completely before or completely after a successful serializable
	 * transaction.
	 * 
	 * @deprecated since 4.0.0. Use {@link IsolationLevel#SERIALIZABLE} instead.
	 */
	SERIALIZABLE(SNAPSHOT, SNAPSHOT_READ, READ_COMMITTED, READ_UNCOMMITTED, NONE, IsolationLevel.SERIALIZABLE,
			IsolationLevel.SNAPSHOT, IsolationLevel.SNAPSHOT_READ, IsolationLevel.READ_COMMITTED,
			IsolationLevel.READ_UNCOMMITTED, IsolationLevel.NONE);

	private final List<? extends IsolationLevel> compatibleLevels;

	private IsolationLevels(IsolationLevel... compatibleLevels) {
		this.compatibleLevels = Arrays.asList(compatibleLevels);
	}

	@Override
	public boolean isCompatibleWith(IsolationLevel otherLevel) {
		return this.equals(otherLevel) || compatibleLevels.contains(otherLevel);
	}

	/**
	 * Determines the first compatible isolation level in the list of supported levels, for the given level. Returns the
	 * level itself if it is in the list of supported levels. Returns null if no compatible level can be found.
	 *
	 * @param level           the {@link IsolationLevel} for which to determine a compatible level.
	 * @param supportedLevels a list of supported isolation levels from which to select the closest compatible level.
	 * @return the given level if it occurs in the list of supported levels. Otherwise, the first compatible level in
	 *         the list of supported isolation levels, or <code>null</code> if no compatible level can be found.
	 * @throws IllegalArgumentException if either one of the input parameters is <code>null</code>.
	 * 
	 * @deprecated since 4.0.0. Use {@link TransactionSettings#getCompatibleIsolationLevel(IsolationLevel, List)}
	 *             instead
	 */
	public static IsolationLevel getCompatibleIsolationLevel(IsolationLevel level,
			List<? extends IsolationLevel> supportedLevels) {
		if (supportedLevels == null) {
			throw new IllegalArgumentException("list of supported levels may not be null");
		}
		if (level == null) {
			throw new IllegalArgumentException("level may not be null");
		}
		if (!supportedLevels.contains(level)) {
			IsolationLevel compatibleLevel = null;
			// see we if we can find a compatible level that is supported
			for (IsolationLevel supportedLevel : supportedLevels) {
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
}
