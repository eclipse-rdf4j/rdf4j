/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Allow a single repository member to control a EvaulationStrategy.
 * 
 * @author James Leigh
 */
@Deprecated
public class RepositoryTripleSource extends org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource {

	public RepositoryTripleSource(RepositoryConnection repo) {
		super(repo);
	}
}
