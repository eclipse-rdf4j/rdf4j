/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
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

	/**
	 * The globally unique transaction settings name. Warning: do not use double underscore (__) in the name.
	 *
	 * @return the name of this setting, typically its canonical class name
	 */
	default String getName() {
		return getClass().getCanonicalName();
	}

	/**
	 * The value for this transaction setting.
	 *
	 * @return a string representation of the value
	 */
	default String getValue() {
		return this.toString();
	}

}
