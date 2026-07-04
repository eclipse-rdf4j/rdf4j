/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * An RDFHandler that adds RDF data to some RDF sink.
 */
public abstract class AbstractRDFInserter extends AbstractRDFHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The contexts to add the statements to. If this variable is a non-empty array, statements will be added to the
	 * corresponding contexts.
	 */
	protected Resource[] contexts = new Resource[0];

	/**
	 * Flag indicating whether blank node IDs should be preserved.
	 */
	private boolean preserveBNodeIDs;

	/**
	 * Map that stores namespaces that are reported during the evaluation of the query. Key is the namespace prefix,
	 * value is the namespace name.
	 */
	private final Map<String, String> namespaceMap;

	/**
	 * Map used to keep track of which blank node IDs have been mapped to which BNode object in case preserveBNodeIDs is
	 * false.
	 */
	private final Map<String, BNode> bNodesMap;

	/**
	 * ValueFactory used to create BNodes.
	 */
	private final ValueFactory valueFactory;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFInserter object that preserves bnode IDs and that does not enforce any context upon statements
	 * that are reported to it.
	 */
	protected AbstractRDFInserter(ValueFactory vf) {
		preserveBNodeIDs = true;
		namespaceMap = new HashMap<>();
		bNodesMap = new HashMap<>();
		valueFactory = vf;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Sets whether this RDFInserter should preserve blank node IDs.
	 *
	 * @param preserveBNodeIDs The new value for this flag.
	 */
	public void setPreserveBNodeIDs(boolean preserveBNodeIDs) {
		this.preserveBNodeIDs = preserveBNodeIDs;
	}

	/**
	 * Checks whether this RDFInserter preserves blank node IDs.
	 */
	public boolean preservesBNodeIDs() {
		return preserveBNodeIDs;
	}

	/**
	 * Enforces the supplied contexts upon all statements that are reported to this RDFInserter.
	 *
	 * @param contexts the contexts to use. Use an empty array (not null!) to indicate no context(s) should be enforced.
	 */
	public void enforceContext(Resource... contexts) {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		this.contexts = Arrays.copyOf(contexts, contexts.length);
	}

	/**
	 * Checks whether this RDFInserter enforces its contexts upon all statements that are reported to it.
	 *
	 * @return <var>true</var> if it enforces its contexts, <var>false</var> otherwise.
	 */
	public boolean enforcesContext() {
		return contexts.length != 0;
	}

	/**
	 * Gets the contexts that this RDFInserter enforces upon all statements that are reported to it (in case
	 * <var>enforcesContext()</var> returns <var>true</var>).
	 *
	 * @return A Resource[] identifying the contexts, or an empty array if no contexts is enforced.
	 */
	public Resource[] getContexts() {
		return Arrays.copyOf(contexts, contexts.length);
	}

	protected abstract void addNamespace(String prefix, String name) throws RDF4JException;

	protected abstract void addStatement(Resource subj, IRI pred, Value obj, Resource ctxt) throws RDF4JException;

	@Override
	public void endRDF() throws RDFHandlerException {
		for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
			String prefix = entry.getKey();
			String name = entry.getValue();

			try {
				addNamespace(prefix, name);
			} catch (RDF4JException e) {
				throw new RDFHandlerException(e);
			}
		}

		namespaceMap.clear();
		bNodesMap.clear();
	}

	@Override
	public void handleNamespace(String prefix, String name) {
		// FIXME: set namespaces directly when they are properly handled wrt
		// rollback
		// don't replace earlier declarations
		if (prefix != null && !namespaceMap.containsKey(prefix)) {
			namespaceMap.put(prefix, name);
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctxt = st.getContext();

		if (!preserveBNodeIDs) {
			if (subj instanceof BNode) {
				subj = mapBNode((BNode) subj);
			}

			if (obj instanceof BNode) {
				obj = mapBNode((BNode) obj);
			}

			if (!enforcesContext() && ctxt instanceof BNode) {
				ctxt = mapBNode((BNode) ctxt);
			}
		}

		try {
			addStatement(subj, pred, obj, ctxt);
		} catch (RDF4JException e) {
			throw new RDFHandlerException(e);
		}
	}

	/**
	 * Maps the supplied BNode, which comes from the data, to a new BNode object. Consecutive calls with equal BNode
	 * objects returns the same object everytime.
	 *
	 * @throws RepositoryException
	 */
	private BNode mapBNode(BNode bNode) {
		BNode result = bNodesMap.get(bNode.getID());

		if (result == null) {
			result = valueFactory.createBNode();
			bNodesMap.put(bNode.getID(), result);
		}

		return result;
	}
}
