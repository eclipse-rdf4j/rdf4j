/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;

/**
 * Tests for {@link Namespace} support in {@link LinkedHashModel} using the abstract tests defined in
 * {@link ModelNamespacesTest}.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class LinkedHashModelNamespacesTest extends ModelNamespacesTest {

	@Override
	protected Model getModelImplementation() {
		return new LinkedHashModel();
	}

}
