/******************************************************************************* 
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.config;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author jeen
 *
 */
public class FedXSailConfig extends BaseSailConfig {

	/**
	 * @param type
	 */
	protected FedXSailConfig(String type) {
		super(type);
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource export(Model graph) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		// TODO Auto-generated method stub

	}

}
