/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.ModelFactory;

/**
 * Creates {@link SimpleUpgradeableModel}.
 *
 */
public class SimpleUpgradeableModelFactory implements ModelFactory {

	@Override
	public SimpleUpgradeableModel createEmptyModel() {
		return new SimpleUpgradeableModel(new LinkedHashModelFactory());
	}

}
