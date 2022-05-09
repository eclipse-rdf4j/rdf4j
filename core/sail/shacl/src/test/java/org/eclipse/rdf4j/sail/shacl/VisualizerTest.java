/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;

public class VisualizerTest {

	@Test
	public void datatype() throws Exception {

		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclDatatype.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(bNode, FOAF.AGE, vf.createLiteral(3));
			connection.commit();

			shaclSail.setLogValidationPlans(true);

			connection.begin();
			BNode bNode2 = vf.createBNode();
			connection.addStatement(bNode2, RDF.TYPE, RDFS.RESOURCE);
			connection.removeStatement(null, bNode, FOAF.AGE, vf.createLiteral(3));
			connection.addStatement(vf.createBNode(), FOAF.AGE, vf.createLiteral(""));

			connection.commit();
		} finally {
			shaclSail.shutDown();
		}

	}

	@Test
	public void maxCount() throws Exception {

		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclMax.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(bNode, RDFS.LABEL, vf.createLiteral(""));
			connection.commit();

			shaclSail.setLogValidationPlans(true);

			connection.begin();
			BNode bNode2 = vf.createBNode();
			connection.addStatement(bNode2, RDF.TYPE, RDFS.RESOURCE);
			connection.removeStatement(null, bNode, RDFS.LABEL, vf.createLiteral(""));

			connection.commit();
		} finally {
			shaclSail.shutDown();
		}

	}

	@Test
	public void minCount() throws Exception {

		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.trig");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(bNode, RDFS.LABEL, vf.createLiteral(""));
			connection.commit();

			shaclSail.setLogValidationPlans(true);

			connection.begin();
			BNode bNode2 = vf.createBNode();
			connection.addStatement(bNode2, RDF.TYPE, RDFS.RESOURCE);
			connection.removeStatement(null, bNode, RDFS.LABEL, vf.createLiteral(""));

			assertThrows(SailException.class, connection::commit);
		} finally {
			shaclSail.shutDown();
		}

	}
}
