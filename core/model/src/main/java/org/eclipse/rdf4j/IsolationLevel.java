/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;

/**
 * A Transaction Isolation Level. Default levels supported by RDF4J are provided by {@link IsolationLevels}, third-party
 * triplestore implementors may choose to add additional IsolationLevel implementations if their triplestore's isolation
 * contract is different from what is provided by default.
 *
 * @author Jeen Broekstra
 */
public interface IsolationLevel extends TransactionSetting {

	/**
	 * Shared constant for the {@link TransactionSetting} name used for isolation levels.
	 */
	static String NAME = IsolationLevel.class.getCanonicalName();

	/**
	 * Verifies if this transaction isolation level is compatible with the supplied other isolation level - that is, if
	 * this transaction isolation level offers at least the same guarantees as the other level. By definition, every
	 * transaction isolation level is compatible with itself.
	 *
	 * @param otherLevel an other isolation level to check compatibility against.
	 * @return true iff this isolation level is compatible with the supplied other isolation level, false otherwise.
	 */
	boolean isCompatibleWith(IsolationLevel otherLevel);

	/**
	 * Get a URI uniquely representing this isolation level.
	 *
	 * @deprecated use getName() and getValue() instead.
	 *
	 * @return a URI that uniquely represents this isolation level.
	 */
	@Deprecated
	IRI getURI();

	@Override
	default String getName() {
		return NAME;
	}

	@Override
	default String getValue() {
		return this.toString();
	}

}
