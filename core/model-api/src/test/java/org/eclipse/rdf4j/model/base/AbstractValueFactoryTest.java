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
package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.ValueFactoryTest;

public class AbstractValueFactoryTest extends ValueFactoryTest {

	@Override
	protected ValueFactory factory() {
		return new GenericValueFactory();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static class GenericValueFactory extends AbstractValueFactory {
	}

}
