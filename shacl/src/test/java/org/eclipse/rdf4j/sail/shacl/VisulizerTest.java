package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class VisulizerTest {

	@Test
	public void datatype() {


		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shaclDatatype.ttl"));
		shaclSail.initialize();


		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(bNode, FOAF.AGE, vf.createLiteral(3));
			connection.commit();

			shaclSail.setDebugPrintPlans(true);

			connection.begin();
			BNode bNode2 = vf.createBNode();
			connection.addStatement(bNode2, RDF.TYPE, RDFS.RESOURCE);
			connection.removeStatement(null, bNode, FOAF.AGE, vf.createLiteral(3));
			connection.addStatement(vf.createBNode(), FOAF.AGE, vf.createLiteral(""));

			connection.commit();
		}


	}

	@Test
	public void maxCount() {


		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shaclMax.ttl"));
		shaclSail.initialize();


		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(bNode, RDFS.LABEL, vf.createLiteral(""));
			connection.commit();

			shaclSail.setDebugPrintPlans(true);

			connection.begin();
			BNode bNode2 = vf.createBNode();
			connection.addStatement(bNode2, RDF.TYPE, RDFS.RESOURCE);
			connection.removeStatement(null, bNode, RDFS.LABEL, vf.createLiteral(""));

			connection.commit();
		}


	}

	@Test(expected = SailException.class)
	public void minCount() {


		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl"));
		shaclSail.initialize();


		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(bNode, RDFS.LABEL, vf.createLiteral(""));
			connection.commit();

			shaclSail.setDebugPrintPlans(true);

			connection.begin();
			BNode bNode2 = vf.createBNode();
			connection.addStatement(bNode2, RDF.TYPE, RDFS.RESOURCE);
			connection.removeStatement(null, bNode, RDFS.LABEL, vf.createLiteral(""));

			connection.commit();
		}


	}
}
