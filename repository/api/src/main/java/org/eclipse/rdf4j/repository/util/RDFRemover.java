/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * An RDFHandler that removes RDF data from a repository.
 */
public class RDFRemover extends AbstractRDFHandler {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * The connection to use for the removal operations.
	 */
	private final RepositoryConnection con;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether the context specified for this RDFRemover should be enforced upon all reported
	 * statements.
	 */
	private boolean enforceContext;

	/**
	 * The context to remove the statements from; <tt>null</tt> to indicate the null context. This context value is used
	 * when enforceContext is set to true.
	 */
	private Resource context;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFRemover object that removes the data from the default context.
	 * 
	 * @param con The connection to use for the removal operations.
	 */
	public RDFRemover(RepositoryConnection con) {
		this.con = con;
		this.enforceContext = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Enforces the supplied context upon all statements that are reported to this RDFRemover.
	 * 
	 * @param context A Resource identifying the context, or <tt>null</tt> for the null context.
	 */
	public void enforceContext(Resource context) {
		this.context = context;
		enforceContext = true;
	}

	/**
	 * Checks whether this RDFRemover enforces its context upon all statements that are reported to it.
	 * 
	 * @return <tt>true</tt> if it enforces its context, <tt>false</tt> otherwise.
	 */
	public boolean enforcesContext() {
		return enforceContext;
	}

	/**
	 * Gets the context identifier that this RDFRemover enforces upon all statements that are reported to it (in case
	 * <tt>enforcesContext()</tt> returns <tt>true</tt>).
	 * 
	 * @return A Resource identifying the context, or <tt>null</tt> if the null context is enforced.
	 */
	public Resource getContext() {
		return context;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		try {
			if (enforceContext) {
				// Override supplied context info
				con.remove(st.getSubject(), st.getPredicate(), st.getObject(), context);
			} else {
				con.remove(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
		} catch (RepositoryException e) {
			throw new RDFHandlerException(e);
		}
	}
}
