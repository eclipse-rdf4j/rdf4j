/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.shacl.AST.NodeShape;

import java.io.IOException;
import java.util.List;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSail extends NotifyingSailWrapper {

	public List<NodeShape> nodeShapes;
	boolean debugPrintPlans = false;

	ShaclSailConfig config = new ShaclSailConfig();
	private static String SH_OR_UPDATE_QUERY;
	private static String SH_OR_NODE_SHAPE_UPDATE_QUERY;

	static {
		try {
			SH_OR_UPDATE_QUERY = IOUtils.toString(ShaclSail.class.getClassLoader().getResourceAsStream("shacl-sparql-inference/sh_or.rq"), "UTF-8");
			SH_OR_NODE_SHAPE_UPDATE_QUERY = IOUtils.toString(ShaclSail.class.getClassLoader().getResourceAsStream("shacl-sparql-inference/sh_or_node_shape.rq"), "UTF-8");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	public ShaclSail(NotifyingSail baseSail, SailRepository shaclSail) {
		super(baseSail);
		try (SailRepositoryConnection shaclSailConnection = shaclSail.getConnection()) {
			runInferencingSparqlQueries(shaclSailConnection);
			nodeShapes = NodeShape.Factory.getShapes(shaclSailConnection);
		}
	}

	private void runInferencingSparqlQueries(SailRepositoryConnection shaclSailConnection) {


		long prevSize;
		long currentSize= shaclSailConnection.size();
		do {
			prevSize = currentSize;
			shaclSailConnection.prepareUpdate(SH_OR_NODE_SHAPE_UPDATE_QUERY).execute();
			shaclSailConnection.prepareUpdate(SH_OR_UPDATE_QUERY).execute();
			currentSize = shaclSailConnection.size();
		}while(prevSize != currentSize);
	}

	@Override
	public NotifyingSailConnection getConnection()
		throws SailException {
		return new ShaclSailConnection(this, super.getConnection(), super.getConnection());
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

	public void setDebugPrintPlans(boolean debugPrintPlans) {
		this.debugPrintPlans = debugPrintPlans;
	}
}

class ShaclSailConfig {

	boolean validationEnabled = true;

}
