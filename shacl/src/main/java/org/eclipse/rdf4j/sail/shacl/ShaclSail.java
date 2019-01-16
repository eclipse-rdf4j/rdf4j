/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.AST.NodeShape;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * A {@link Sail} implementation that adds support for the Shapes Constraint Language (SHACL)
 *
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 * @see <a href="https://www.w3.org/TR/shacl/">SHACL W3C Recommendation</a>
 */
public class ShaclSail extends NotifyingSailWrapper {

	/**
	 * The virtual context identifier for persisting the SHACL shapes information.
	 */
	@SuppressWarnings("WeakerAccess")
	public final static IRI SHAPE_GRAPH = SimpleValueFactory
		.getInstance()
		.createIRI("http://rdf4j.org/schema/schacl#ShapeGraph");

	List<NodeShape> nodeShapes;

	boolean debugPrintPlans = false;

	ShaclSailConfig config = new ShaclSailConfig();

	private static String SH_OR_UPDATE_QUERY;

	private static String SH_OR_NODE_SHAPE_UPDATE_QUERY;

	/**
	 * an initialized {@link Repository} for storing/retrieving Shapes data
	 */
	private SailRepository shapesRepo;

	static {
		try {
			SH_OR_UPDATE_QUERY = IOUtils.toString(
				ShaclSail
					.class
					.getClassLoader()
					.getResourceAsStream("shacl-sparql-inference/sh_or.rq"),
				"UTF-8");
			SH_OR_NODE_SHAPE_UPDATE_QUERY = IOUtils.toString(
				ShaclSail
					.class
					.getClassLoader()
					.getResourceAsStream("shacl-sparql-inference/sh_or_node_shape.rq"),
				"UTF-8");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	public ShaclSail(NotifyingSail baseSail) {
		super(baseSail);
		String path = null;
		if (baseSail.getDataDir() != null) {
			path = baseSail.getDataDir().getPath();
		} else {
			try {
				path = Files.createTempDirectory("shacl-shapes").toString();
			} catch (IOException e) {
				throw new SailConfigException(e);
			}
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		path = path + "-shapes-graph/";

		shapesRepo = new SailRepository(new MemoryStore(new File(path)));
		shapesRepo.initialize();
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
		try (SailRepositoryConnection shapesRepoConnection = shapesRepo.getConnection()) {
			refreshShapes(shapesRepoConnection);
		}

	}

	void refreshShapes(SailRepositoryConnection shapesRepoConnection) throws SailException {
		try (SailRepositoryConnection beforeCommitConnection = shapesRepo.getConnection()) {
			long size = beforeCommitConnection.size();
			if (size > 0) {
				// Our inferencer both adds and removes statements.
				// To support updates I recommend having two graphs, one raw one with the unmodified data.
				// Then copy all that data into a new graph, run inferencing on that graph and use it to generate the java objects
				throw new IllegalStateException("ShaclSail does not support modifying shapes that are already loaded or loading more shapes");
			}
		}

		runInferencingSparqlQueries(shapesRepoConnection);
		nodeShapes = NodeShape.Factory.getShapes(shapesRepoConnection);
	}

	@Override
	public void shutDown() throws SailException {
		try {
			shapesRepo.shutDown();
		} finally {
			shapesRepo = null;
		}
		super.shutDown();
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		return new ShaclSailConnection(this, super.getConnection(), super.getConnection(), shapesRepo.getConnection());
	}

	public void disableValidation() {
		config.validationEnabled = false;
	}

	public void enableValidation() {
		config.validationEnabled = true;
	}

	public boolean isDebugPrintPlans() {
		return debugPrintPlans;
	}

	@SuppressWarnings("WeakerAccess")
	public void setDebugPrintPlans(boolean debugPrintPlans) {
		this.debugPrintPlans = debugPrintPlans;
	}

	List<NodeShape> getNodeShapes() {
		return nodeShapes;
	}

	private void runInferencingSparqlQueries(SailRepositoryConnection shaclSailConnection) {

		long prevSize;
		long currentSize = shaclSailConnection.size();
		do {
			prevSize = currentSize;
			shaclSailConnection.prepareUpdate(SH_OR_NODE_SHAPE_UPDATE_QUERY).execute();
			shaclSailConnection.prepareUpdate(SH_OR_UPDATE_QUERY).execute();
			currentSize = shaclSailConnection.size();
		}
		while (prevSize != currentSize);
	}

}

class ShaclSailConfig {

	boolean validationEnabled = true;

}
