/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.base.SailDataset;

/**
 * Extension of {@link SailDatasetTripleSource} to make the class visible
 * 
 * @author as
 */
public class SailDatasetTripleSourceExt extends SailDatasetTripleSource
{

	public SailDatasetTripleSourceExt(ValueFactory vf, SailDataset dataset)
	{
		super(vf, dataset);
	}

}
