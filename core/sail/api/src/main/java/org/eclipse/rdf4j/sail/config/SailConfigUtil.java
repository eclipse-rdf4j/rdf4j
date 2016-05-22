/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;

public class SailConfigUtil {

	public static SailImplConfig parseRepositoryImpl(Model m, Resource implNode)
		throws SailConfigException
	{
		try {
			Literal typeLit = Models.objectLiteral(m.filter(implNode, SailConfigSchema.SAILTYPE, null));

			if (typeLit != null) {
				SailFactory factory = SailRegistry.getInstance().get(typeLit.getLabel());

				if (factory != null) {
					SailImplConfig implConfig = factory.getConfig();
					implConfig.parse(m, implNode);
					return implConfig;
				}
				else {
					throw new SailConfigException("Unsupported Sail type: " + typeLit.getLabel());
				}
			}

			return null;
		}
		catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
