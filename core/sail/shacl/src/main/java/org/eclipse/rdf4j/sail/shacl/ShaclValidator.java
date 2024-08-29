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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShape;
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
	private static Resource[] SHAPE_CONTEXTS = ALL_CONTEXTS;

	public static ValidationReport validate(Sail dataRepo, Sail shapesRepo) {

		List<ContextWithShape> shapes;
		try (SailConnection shapesConnection = shapesRepo.getConnection()) {
			shapesConnection.begin(IsolationLevels.NONE);
			try (ShapeSource shapeSource = new CombinedShapeSource(shapesConnection,
					shapesConnection)) {
				Stream<ShapeSource.ShapesGraph> allShapeContexts = shapeSource
						.withContext(SHAPE_CONTEXTS)
						.getAllShapeContexts();
				if (SHAPE_CONTEXTS.length == 0) {
					allShapeContexts = Stream.concat(allShapeContexts,
							Stream.of(new ShapeSource.ShapesGraph(RDF4J.NIL)));
				}
				List<ContextWithShape> parsed = allShapeContexts
						.map(context -> Shape.Factory.parse(shapeSource.withContext(context.getShapesGraph()), context,
								new Shape.ParseSettings(true, true)))
						.flatMap(List::stream)
						.collect(Collectors.toList());

				shapes = Shape.Factory.getShapes(parsed).stream().distinct().collect(Collectors.toList());

				if (logger.isDebugEnabled()) {
					for (ContextWithShape shape : shapes) {
						logger.debug("Using data graph(s) {} and shape graph(s) {} with shape {}",
								Arrays.toString(shape.getDataGraph()), Arrays.toString(shape.getShapeGraph()),
								shape.getShape());
					}
				}

			}
			shapesConnection.commit();
		} catch (Throwable e) {
			logger.warn("Failed to read shapes", e);
			throw e;
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
		} catch (Throwable e) {
			logger.warn("Failed to validate shapes", e);
			throw e;
		}

	}

	private static ValidationReport performValidation(List<ContextWithShape> shapes,
			ConnectionsGroup connectionsGroup) {

		List<ValidationResultIterator> collect = shapes
				.stream()
				.map(contextWithShape -> new ShapeValidationContainer(
						contextWithShape.getShape(),
						() -> contextWithShape.getShape()
								.generatePlans(connectionsGroup,
										new ValidationSettings(contextWithShape.getDataGraph(), false, true, false)),
						false, false, 1000, false, false, logger,
						connectionsGroup)
				)
				.filter(ShapeValidationContainer::hasPlanNode)
				.map(ShapeValidationContainer::performValidation)
				.collect(Collectors.toList());

		return new LazyValidationReport(collect, 10000);

	}

}
