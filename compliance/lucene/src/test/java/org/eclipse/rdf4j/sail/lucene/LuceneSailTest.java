/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;

/**
 * @author jeen
 *
 */
public class LuceneSailTest extends AbstractLuceneSailTest {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailTest#configure(org.eclipse.rdf4j.sail.lucene.LuceneSail)
	 */
	@Override
	protected void configure(LuceneSail sail) throws IOException {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
	}

}
