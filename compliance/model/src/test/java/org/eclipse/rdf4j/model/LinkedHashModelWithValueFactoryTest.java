/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import junit.framework.Test;

/**
 * ValueFactory is not serializable. This test ensures that the LinkedHashModel's getValueFactory() does not
 * try to serialize a ValueFactory with it.
 * 
 * @author James Leigh
 */
public class LinkedHashModelWithValueFactoryTest extends ModelTest {

	public static Test suite()
		throws Exception
	{
		return ModelTest.suite(LinkedHashModelWithValueFactoryTest.class);
	}

	public LinkedHashModelWithValueFactoryTest(String name) {
		super(name);
	}

	@Override
	public Model makeEmptyModel() {
		LinkedHashModel model = new LinkedHashModel();
		model.getValueFactory();
		return model;
	}
}
