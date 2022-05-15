/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class XoneConstraintComponent extends AbstractConstraintComponent {
	List<Shape> xone;

	public XoneConstraintComponent(Resource id, ShapeSource shapeSource, Cache cache, ShaclSail shaclSail) {
		super(id);
		xone = ShaclAstLists.toList(shapeSource, id, Resource.class)
				.stream()
				.map(r -> new ShaclProperties(r, shapeSource))
				.map(p -> {
					if (p.getType() == SHACL.NODE_SHAPE) {
						return NodeShape.getInstance(p, shapeSource, cache, shaclSail);
					} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
						return PropertyShape.getInstance(p, shapeSource, cache, shaclSail);
					}
					throw new IllegalStateException("Unknown shape type for " + p.getId());
				})
				.collect(Collectors.toList());

	}

	public XoneConstraintComponent(XoneConstraintComponent xoneConstraintComponent) {
		super(xoneConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.XONE, getId());

		if (!cycleDetection.contains(getId())) {
			cycleDetection.add(getId());
			xone.forEach(o -> o.toModel(null, null, model, cycleDetection));
		}

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(xone.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
		}
	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		for (Shape shape : xone) {
			shape.setTargetChain(targetChain.setOptimizable(false));
		}
	}

	public List<Shape> getXone() {
		return Collections.unmodifiableList(xone);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.XoneConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {

		XoneConstraintComponent constraintComponent = new XoneConstraintComponent(this);
		constraintComponent.xone = xone.stream()
				.map(ConstraintComponent::deepClone)
				.map(a -> ((Shape) a))
				.collect(Collectors.toList());
		return constraintComponent;
	}
}
