/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 
 * Utility functions for working with {@link TransactionSetting}s.
 * 
 * @author Jeen Broekstra
 *
 * @since 4.0.0
 */
public class TransactionSettings {

	/**
	 * A list of standard {@link IsolationLevel isolation levels} supported by RDF4J, in order of ascending strength.
	 */
	public static final List<IsolationLevel> STANDARD_ISOLATION_LEVELS = Arrays.asList(
			IsolationLevel.NONE,
			IsolationLevel.READ_UNCOMMITTED,
			IsolationLevel.READ_COMMITTED,
			IsolationLevel.SNAPSHOT_READ,
			IsolationLevel.SNAPSHOT,
			IsolationLevel.SERIALIZABLE
	);

	/**
	 * Retrieve the first {@link TransactionSetting} from the supplied list that matches the supplied name.
	 * 
	 * @param name     the name of the transaction setting to retrieve
	 * @param settings a list of {@link TransactionSetting}s to find a match in
	 * @return the matching {@link TransactionSetting} or {@link Optional#empty()} if no match is found.
	 */
	public static Optional<? extends TransactionSetting> getSettingForName(String name,
			List<? extends TransactionSetting> settings) {
		for (TransactionSetting setting : settings) {
			if (setting.getName().equals(name)) {
				return Optional.of(setting);
			}
		}
		return Optional.empty();
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
	 * @see IsolationLevel
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
