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

import java.util.Optional;

/**
 * A TransactionSettingFactory returns a {@link TransactionSetting} implementation for a given value.
 *
 * @author Jeen Broekstra
 * @see TransactionSettingRegistry
 *
 * @since 3.3.0
 */
public interface TransactionSettingFactory {

	/**
	 * Name of {@link TransactionSetting} this factory produces
	 *
	 * @return the name of the {@link TransactionSetting} produced by this factory
	 */
	String getName();

	/**
	 * Retrieve a {@link TransactionSetting} with the supplied value.
	 *
	 * @param value the {@link TransactionSetting} value
	 * @return an optional {@link TransactionSetting}, empty if the factory can not produce one for the given value.
	 */
	Optional<TransactionSetting> getTransactionSetting(String value);

}
