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
 * Creates {@link DynamicModel}.
 *
 */
public class DynamicModelFactory implements ModelFactory {

	@Override
	public DynamicModel createEmptyModel() {
		return new DynamicModel(new LinkedHashModelFactory());
	}

}
