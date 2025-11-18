/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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
 * {@link TransactionSettingFactory} for {@link IsolationLevel}s exposed by the RDF4J API.
 */
public class IsolationLevelFactory implements TransactionSettingFactory {

	@Override
	public String getName() {
		return IsolationLevel.NAME;
	}

	@Override
	public Optional<TransactionSetting> getTransactionSetting(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(IsolationLevels.valueOf(value.trim()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
