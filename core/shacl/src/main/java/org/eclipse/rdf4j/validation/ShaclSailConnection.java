/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.AST.Shape;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

import java.util.List;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper{

	public ShaclSail sail;

	public ShaclSailConnection(ShaclSail shaclSail, NotifyingSailConnection connection) {
		super(connection);
		this.sail = shaclSail;
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		super.begin(level);
	}

	@Override
	public void commit() throws SailException {
		super.commit();

		for (Shape shape : sail.shapes) {
			List<PlanNode> planNodes = shape.generatePlans(this, shape);
			for (PlanNode planNode :planNodes){
				boolean valid = planNode.validate();
				if(!valid){
					throw new SailException("invalid for shacl");
				}
			}
		}
	}

	protected Model createModel(){
		return new TreeModel();
	};

}
