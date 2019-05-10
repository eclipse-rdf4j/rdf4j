/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LanguageInFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Håvard Ottestad
 */
public class LanguageInPropertyShape extends PathPropertyShape {

	private final Set<String> languageIn;
	private static final Logger logger = LoggerFactory.getLogger(LanguageInPropertyShape.class);

	LanguageInPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			Resource languageIn) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.languageIn = toList(connection, languageIn).stream().map(Value::stringValue).collect(Collectors.toSet());
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNodeProvider overrideTargetNode) {
		if (deactivated) {
			return null;
		}

		PlanNode invalidValues = StandardisedPlanHelper.getGenericSingleObjectPlan(shaclSailConnection, nodeShape,
				(parent) -> new LanguageInFilter(parent, languageIn), this, overrideTargetNode);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.LanguageInConstraintComponent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		LanguageInPropertyShape that = (LanguageInPropertyShape) o;
		return languageIn.equals(that.languageIn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), languageIn);
	}

	@Override
	public String toString() {
		return "LanguageInPropertyShape{" +
				"languageIn=" + Arrays.toString(languageIn.toArray()) +
				", path=" + getPath() +
				'}';
	}
}
