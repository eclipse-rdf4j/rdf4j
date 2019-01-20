/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
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
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * A {@link Sail} implementation that adds support for the Shapes Constraint Language (SHACL).
 * <p>
 * The ShaclSail looks for SHACL shape data in a special named graph {@link RDF4J#SHACL_SHAPE_GRAPH}.
 *
 *
 * <h4>Working example</h4>
 *
 * <pre>
 *import ch.qos.logback.classic.Level;
 *import ch.qos.logback.classic.Logger;
 *import org.eclipse.rdf4j.model.Model;
 *import org.eclipse.rdf4j.model.vocabulary.RDF4J;
 *import org.eclipse.rdf4j.repository.RepositoryException;
 *import org.eclipse.rdf4j.repository.sail.SailRepository;
 *import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
 *import org.eclipse.rdf4j.rio.RDFFormat;
 *import org.eclipse.rdf4j.rio.Rio;
 *import org.eclipse.rdf4j.sail.memory.MemoryStore;
 *import org.eclipse.rdf4j.sail.shacl.ShaclSail;
 *import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
 *import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
 *import org.slf4j.LoggerFactory;
 *
 *import java.io.IOException;
 *import java.io.StringReader;
 *
 *public class ShaclSampleCode {
 *
 *	public static void main(String[] args) throws IOException {
 *
 *		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
 *
 *		//Logger root = (Logger) LoggerFactory.getLogger(ShaclSail.class.getName());
 *		//root.setLevel(Level.INFO);
 *
 *		//shaclSail.setLogValidationPlans(true);
 *		//shaclSail.setGlobalLogValidationExecution(true);
 *		//shaclSail.setLogValidationViolations(true);
 *
 *		SailRepository sailRepository = new SailRepository(shaclSail);
 *		sailRepository.init();
 *
 *		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
 *
 *			connection.begin();
 *
 *			StringReader shaclRules = new StringReader(
 *				String.join(&quot;\n&quot;, &quot;&quot;,
 *					&quot;@prefix ex: &lt;http://example.com/ns#&gt; .&quot;,
 *					&quot;@prefix sh: &lt;http://www.w3.org/ns/shacl#&gt; .&quot;,
 *					&quot;@prefix xsd: &lt;http://www.w3.org/2001/XMLSchema#&gt; .&quot;,
 *					&quot;@prefix foaf: &lt;http://xmlns.com/foaf/0.1/&gt;.&quot;,
 *
 *					&quot;ex:PersonShape&quot;,
 *					&quot;	a sh:NodeShape  ;&quot;,
 *					&quot;	sh:targetClass foaf:Person ;&quot;,
 *					&quot;	sh:property ex:PersonShapeProperty .&quot;,
 *
 *					&quot;ex:PersonShapeProperty &quot;,
 *					&quot;	sh:path foaf:age ;&quot;,
 *					&quot;	sh:datatype xsd:int ;&quot;,
 *					&quot;	sh:maxCount 1 ;&quot;,
 *					&quot;	sh:minCount 1 .&quot;
 *				));
 *
 *			connection.add(shaclRules, &quot;&quot;, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
 *			connection.commit();
 *
 *			connection.begin();
 *
 *			StringReader invalidSampleData = new StringReader(
 *				String.join(&quot;\n&quot;, &quot;&quot;,
 *					&quot;@prefix ex: &lt;http://example.com/ns#&gt; .&quot;,
 *					&quot;@prefix foaf: &lt;http://xmlns.com/foaf/0.1/&gt;.&quot;,
 *					&quot;@prefix xsd: &lt;http://www.w3.org/2001/XMLSchema#&gt; .&quot;,
 *
 *					&quot;ex:peter a foaf:Person ;&quot;,
 *					&quot;	foaf:age 20, \&quot;30\&quot;^^xsd:int  .&quot;
 *
 *				));
 *
 *			connection.add(invalidSampleData, &quot;&quot;, RDFFormat.TURTLE);
 *			try {
 *				connection.commit();
 *			} catch (RepositoryException exception) {
 *				Throwable cause = exception.getCause();
 *				if (cause instanceof ShaclSailValidationException) {
 *					ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
 *					Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
 *					// use validationReport or validationReportModel to understand validation violations
 *
 *					Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
 *				}
 *				throw exception;
 *			}
 *		}
 *	}
 *}
 *</pre>
 *
 *
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 * @see <a href="https://www.w3.org/TR/shacl/">SHACL W3C Recommendation</a>
 */
public class ShaclSail extends NotifyingSailWrapper {

	private List<NodeShape> nodeShapes;


	final ShaclSailConfig config = new ShaclSailConfig();

	private static String SH_OR_UPDATE_QUERY;

	private static String SH_OR_NODE_SHAPE_UPDATE_QUERY;

	/**
	 * an initialized {@link Repository} for storing/retrieving Shapes data
	 */
	private SailRepository shapesRepo;

	static {
		try {
			SH_OR_UPDATE_QUERY = IOUtils.toString(
					ShaclSail.class.getClassLoader().getResourceAsStream("shacl-sparql-inference/sh_or.rq"),
					"UTF-8");
			SH_OR_NODE_SHAPE_UPDATE_QUERY = IOUtils.toString(
					ShaclSail.class.getClassLoader().getResourceAsStream("shacl-sparql-inference/sh_or_node_shape.rq"),
					"UTF-8");
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	public ShaclSail(NotifyingSail baseSail) {
		super(baseSail);
		String path = null;
		if (baseSail.getDataDir() != null) {
			path = baseSail.getDataDir().getPath();
		}
		else {
			try {
				path = Files.createTempDirectory("shacl-shapes").toString();
			}
			catch (IOException e) {
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

	public ShaclSail() {
		super();
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
				throw new IllegalStateException(
						"ShaclSail does not support modifying shapes that are already loaded or loading more shapes");
			}
		}

		runInferencingSparqlQueries(shapesRepoConnection);
		nodeShapes = NodeShape.Factory.getShapes(shapesRepoConnection);
	}

	@Override
	public void shutDown() throws SailException {
		try {
			shapesRepo.shutDown();
		}
		finally {
			shapesRepo = null;
		}
		super.shutDown();
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		return new ShaclSailConnection(this, super.getConnection(), super.getConnection(),
				shapesRepo.getConnection());
	}


	/**
	 * Disable the SHACL validation on commit()
	 */
	public void disableValidation() {
		config.validationEnabled = false;
	}

	/**
	 * Enabled the SHACL validation on commit()
	 */
	public void enableValidation() {
		config.validationEnabled = true;
	}

	public boolean isLogValidationPlans() {
		return config.logValidationPlans;
	}

	public boolean isIgnoreNoShapesLoadedException() {
		return config.ignoreNoShapesLoadedException;
	}

	/**
	 * Check if shapes have been loaded into the shapes graph before other data is added
	 * @param ignoreNoShapesLoadedException
	 */
	public void setIgnoreNoShapesLoadedException(boolean ignoreNoShapesLoadedException) {
		config.ignoreNoShapesLoadedException = ignoreNoShapesLoadedException;
	}

	/**
	 * Log (INFO) the executed validation plans as GraphViz DOT
	 * @param logValidationPlans
	 */
	@SuppressWarnings("WeakerAccess")
	public void setLogValidationPlans(boolean logValidationPlans) {
		config.logValidationPlans = logValidationPlans;
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

	/**
	 * 	Log (INFO) every execution step of the SHACL validation
	 * 	This is fairly costly and should not be used in production.
	 * @param loggingEnabled
	 */
	public void setGlobalLogValidationExecution(boolean loggingEnabled) {
		LoggingNode.loggingEnabled = loggingEnabled;
	}

	public boolean isGlobalLogValidationExecution() {
		return LoggingNode.loggingEnabled;
	}

	public boolean isLogValidationViolations() {
		return config.logValidationViolations;
	}

	/**
	 * Log (INFO) a list og violations and the triples that caused the violations (BETA)
	 * @param logValidationViolations
	 */
	public void setLogValidationViolations(boolean logValidationViolations) {
		config.logValidationViolations = logValidationViolations;
	}

}

class ShaclSailConfig {

	boolean logValidationPlans = false;
	boolean logValidationViolations = false;
	boolean ignoreNoShapesLoadedException = false;
	boolean validationEnabled = true;

}
