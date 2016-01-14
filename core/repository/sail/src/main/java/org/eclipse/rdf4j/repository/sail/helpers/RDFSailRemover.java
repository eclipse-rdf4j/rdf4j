/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.helpers;

import java.util.Set;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;

/**
 * An Sail-specific RDFHandler that removes RDF data from a repository. To be
 * used in combination with SPARQL DELETE DATA only.
 * 
 * @author jeen
 */
class RDFSailRemover extends AbstractRDFHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The connection to use for the remove operations.
	 */
	private final SailConnection con;

	private final ValueFactory vf;

	private final UpdateContext uc;

	/**
	 * The contexts to remove the statements from. If this variable is a
	 * non-empty array, statements will be removed from the corresponding
	 * contexts.
	 */
	private Resource[] contexts = new Resource[0];

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFSailRemover object.
	 * 
	 * @param con
	 *        The connection to use for the remove operations.
	 */
	public RDFSailRemover(SailConnection con, ValueFactory vf, UpdateContext uc) {
		this.con = con;
		this.vf = vf;
		this.uc = uc;

	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Enforces the supplied contexts upon all statements that are reported to
	 * this RDFSailRemover.
	 * 
	 * @param contexts
	 *        the contexts to use. Use an empty array (not null!) to indicate no
	 *        context(s) should be enforced.
	 */
	public void enforceContext(Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);
		this.contexts = contexts;
	}

	/**
	 * Checks whether this RDFRemover enforces its contexts upon all statements
	 * that are reported to it.
	 * 
	 * @return <tt>true</tt> if it enforces its contexts, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean enforcesContext() {
		return contexts.length != 0;
	}

	/**
	 * Gets the contexts that this RDFRemover enforces upon all statements that
	 * are reported to it (in case <tt>enforcesContext()</tt> returns
	 * <tt>true</tt>).
	 * 
	 * @return A Resource[] identifying the contexts, or <tt>null</tt> if no
	 *         contexts is enforced.
	 */
	public Resource[] getContexts() {
		return contexts;
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctxt = st.getContext();

		try {
			if (enforcesContext()) {
				con.removeStatement(uc, subj, pred, obj, contexts);
			}
			else {
				if (ctxt == null) {
					final Set<IRI> removeGraphs = uc.getDataset().getDefaultRemoveGraphs();
					if (!removeGraphs.isEmpty()) {
						con.removeStatement(uc, subj, pred, obj, new IRI[removeGraphs.size()]);
					}
					else {
						con.removeStatement(uc, subj, pred, obj);
					}
				}
				else {
					con.removeStatement(uc, subj, pred, obj, ctxt);
				}
			}
		}
		catch (SailException e) {
			throw new RDFHandlerException(e);
		}
	}
}
