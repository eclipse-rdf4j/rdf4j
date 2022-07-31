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
package org.eclipse.rdf4j.sail.shacl;

import java.util.Optional;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.common.transaction.TransactionSettingFactory;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach;

/**
 * Factory class for producing instances of {@link ValidationApproach} from string values.
 *
 * @author Jeen Broekstra
 *
 */
public class ValidationApproachFactory implements TransactionSettingFactory {

	@Override
	public String getName() {
		return ValidationApproach.class.getCanonicalName();
	}

	@Override
	public Optional<TransactionSetting> getTransactionSetting(String value) {
		try {
			return Optional.of(ValidationApproach.valueOf(value));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}
