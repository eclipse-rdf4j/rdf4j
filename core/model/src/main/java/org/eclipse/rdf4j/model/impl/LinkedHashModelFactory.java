/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.ModelFactory;

import java.io.Serializable;

/**
 * Creates {@link LinkedHashModel}.
 *
 * @author James Leigh
 */
public class LinkedHashModelFactory implements ModelFactory, Serializable {

	private static final long serialVersionUID = -9152104133818783614L;

	@Override
	public LinkedHashModel createEmptyModel() {
		return new LinkedHashModel();
	}

}
