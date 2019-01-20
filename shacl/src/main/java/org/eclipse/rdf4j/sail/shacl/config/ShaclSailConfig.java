/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;


/**
 * A {@link SailImplConfig} for {@link ShaclSail}. 
 * 
 * @author Jeen Broekstra
 *
 */
public class ShaclSailConfig extends AbstractDelegatingSailImplConfig {

	public ShaclSailConfig() {
		super(ShaclSailFactory.SAIL_TYPE);
	}
}
