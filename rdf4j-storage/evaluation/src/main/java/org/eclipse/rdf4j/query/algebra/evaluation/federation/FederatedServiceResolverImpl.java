/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

/**
 * The {@link FederatedServiceResolverImpl} is used to manage a set of {@link FederatedService} instances, which are
 * used to evaluate SERVICE expressions for particular service Urls.
 * <p>
 * Lookup can be done via the serviceUrl using the method {@link #getService(String)}. If there is no service for the
 * specified url, a {@link SPARQLFederatedService} is created and registered for future use.
 * 
 * @author Andreas Schwarte
 * @author James Leigh
 * @deprecated since 2.3 use {@link org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver}
 */
@Deprecated
public class FederatedServiceResolverImpl extends org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver {
}
