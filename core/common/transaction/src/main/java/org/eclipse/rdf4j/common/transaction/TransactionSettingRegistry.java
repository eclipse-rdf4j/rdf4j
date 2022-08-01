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

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

/**
 *
 * A {@link ServiceRegistry} for creating/retrieving {@link TransactionSetting}s in a transparent way.
 *
 * @author Jeen Broekstra
 *
 * @since 3.3.0
 *
 */
public class TransactionSettingRegistry extends ServiceRegistry<String, TransactionSettingFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class TransactionSettingRegistryHolder {
		public static final TransactionSettingRegistry instance = new TransactionSettingRegistry();
	}

	/**
	 * Gets the TransactionSettingRegistry singleton.
	 *
	 * @return The registry singleton.
	 */
	public static TransactionSettingRegistry getInstance() {
		return TransactionSettingRegistryHolder.instance;
	}

	/**
	 * private constructor: singleton
	 */
	private TransactionSettingRegistry() {
		super(TransactionSettingFactory.class);
	}

	@Override
	protected String getKey(TransactionSettingFactory factory) {
		return factory.getName();
	}

}
