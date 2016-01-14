/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene3;

import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailIndexedPropertiesTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene3.LuceneIndex;

public class LuceneSailIndexedPropertiesTest extends AbstractLuceneSailIndexedPropertiesTest {
	protected void configure(LuceneSail sail)
	{
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
	}
}
