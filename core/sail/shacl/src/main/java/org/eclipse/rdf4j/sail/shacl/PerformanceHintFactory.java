/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import java.util.Optional;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.common.transaction.TransactionSettingFactory;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint;

/**
 * Factory class for producing instances of {@link PerformanceHint} from string values.
 *
 * @author Håvard Ottestad
 *
 */
public class PerformanceHintFactory implements TransactionSettingFactory {

	@Override
	public String getName() {
		return PerformanceHint.class.getCanonicalName();
	}

	@Override
	public Optional<TransactionSetting> getTransactionSetting(String value) {
		try {
			return Optional.of(PerformanceHint.valueOf(value));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}
