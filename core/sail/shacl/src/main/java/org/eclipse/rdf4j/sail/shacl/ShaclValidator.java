/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShapes;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.lazy.LazyValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.lazy.ValidationResultIterator;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.VerySimpleRdfsBackwardsChainingConnection;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.CombinedShapeSource;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Experimental
public class ShaclValidator {

	private static final Resource[] ALL_CONTEXTS = {};
	private static final Logger logger = LoggerFactory.getLogger(ShaclValidator.class);

	// tests can write to this field using reflection
	@SuppressWarnings("FieldMayBeFinal")
	private static Resource[] CONTEXTS = {};

	public static ValidationReport validate(Sail dataRepo, Sail shapesRepo) {

		List<ContextWithShapes> shapes;
		try (SailConnection shapesConnection = shapesRepo.getConnection()) {
			shapesConnection.begin(IsolationLevels.NONE);
			try (ShapeSource shapeSource = new CombinedShapeSource(shapesConnection,
					shapesConnection)) {
				shapes = Shape.Factory.getShapes(shapeSource.withContext(CONTEXTS),
						new Shape.ParseSettings(true, true));
			}
			shapesConnection.commit();
		}

		try (SailConnection dataRepoConnection = dataRepo.getConnection()) {

			RdfsSubClassOfReasoner reasoner;

			try (SailConnection shapesConnection = shapesRepo.getConnection()) {
				reasoner = RdfsSubClassOfReasoner.createReasoner(
						dataRepoConnection, shapesConnection,
						new ValidationSettings(ALL_CONTEXTS, false, true, false));
			}

			VerySimpleRdfsBackwardsChainingConnection verySimpleRdfsBackwardsChainingConnection = new VerySimpleRdfsBackwardsChainingConnection(
					dataRepoConnection, reasoner);

			return performValidation(shapes, new ConnectionsGroup(verySimpleRdfsBackwardsChainingConnection, null,
					null, null, new Stats(), () -> reasoner,
					new ShaclSailConnection.Settings(true, true, true, IsolationLevels.NONE), true));
		}

	}

	private static ValidationReport performValidation(List<ContextWithShapes> shapes,
			ConnectionsGroup connectionsGroup) {

		List<ValidationResultIterator> collect = shapes
				.stream()
				.flatMap(contextWithShapes -> contextWithShapes
						.getShapes()
						.stream()
						.map(shape -> new ShapeValidationContainer(
								shape,
								() -> shape.generatePlans(connectionsGroup,
										new ValidationSettings(contextWithShapes.getDataGraph(), false, true, false)),
								false, false, 1000, false, logger)
						)
				)
				.filter(ShapeValidationContainer::hasPlanNode)
				.map(ShapeValidationContainer::performValidation)
				.collect(Collectors.toList());

		return new LazyValidationReport(collect, 10000);

	}

}
