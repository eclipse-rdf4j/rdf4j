/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin.config;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public class SpinSailConfig extends AbstractDelegatingSailImplConfig {

	/**
	 * Flag to indicate if the SPIN engine should initialize using the full deductive closure of the SPIN
	 * axioms. This is necessary if the underlying Sail stack does not provide RDFS inferencing.
	 */
	private boolean axiomClosureNeeded = true;

	public SpinSailConfig() {
		super(SpinSailFactory.SAIL_TYPE);
	}

	public SpinSailConfig(SailImplConfig delegate) {
		super(SpinSailFactory.SAIL_TYPE, delegate);
		if ("openrdf:ForwardChainingRDFSInferencer".equals(delegate.getType())) {
			setAxiomClosureNeeded(false);
		}
	}

	public SpinSailConfig(SailImplConfig delegate, boolean axiomClosureNeeded) {
		super(SpinSailFactory.SAIL_TYPE, delegate);
		setAxiomClosureNeeded(axiomClosureNeeded);
	}

	public boolean isAxiomClosureNeeded() {
		return axiomClosureNeeded;
	}

	public void setAxiomClosureNeeded(boolean axiomClosureNeeded) {
		this.axiomClosureNeeded = axiomClosureNeeded;
	}

	@Override
	public void parse(Model m, Resource implNode)
		throws SailConfigException
	{
		super.parse(m, implNode);

		try {
			Literal lit = Models.objectLiteral(m.filter(implNode, SpinSailSchema.AXIOM_CLOSURE_NEEDED, null));

			if (lit != null) {
				setAxiomClosureNeeded(lit.booleanValue());
			}
		}
		catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
