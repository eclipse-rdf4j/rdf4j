/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Base class for any federation test, this class is self-contained with regard to testing if run in a distinct JVM.
 *
 * @author as
 *
 */
public abstract class SPARQLBaseTest extends SPARQLServerBaseTest {

	@RegisterExtension
	public FedXRule fedxRule = new FedXRule();

	protected SPARQLBaseTest() {
		super();
		initFedXConfig();
	}

	/**
	 * Can be used to initialize the {@link FedXRule} from sub clases
	 */
	protected void initFedXConfig() {

	}

	protected Set<Statement> getStatements(Resource subj, IRI pred, Value obj) throws Exception {

		Set<Statement> res = new HashSet<>();
		try (RepositoryConnection conn = fedxRule.getRepository()
				.getConnection()) {
			try (RepositoryResult<Statement> stmts = conn.getStatements(subj, pred, obj, false)) {
				while (stmts.hasNext()) {
					res.add(stmts.next());
				}
			}
		}
		return res;
	}

	@Override
	protected FederationContext federationContext() {
		return fedxRule.getFederationContext();
	}
}
