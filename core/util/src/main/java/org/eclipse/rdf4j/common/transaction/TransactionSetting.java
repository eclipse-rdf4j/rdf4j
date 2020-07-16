/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

/**
 * A configuration setting that can be passed at the beginning of a repository transaction to influence behavior within
 * the scope of that transaction only.
 *
 * @author Håvard Ottestad
 * @author Jeen Broekstra
 */
public interface TransactionSetting {

	// FIXME we should perhaps use getURI (with an IRI return type) to uniquely identify settings. Same as what we do
	// for IsolationLevels.
	default String getName() {
		return getClass().getCanonicalName();
	}

	default String getValue() {
		return this.toString();
	}

}
