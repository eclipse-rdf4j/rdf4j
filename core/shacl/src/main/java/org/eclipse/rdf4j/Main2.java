package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.ShaclSail;

public class Main2 {


	public static void main(String[] args) {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.initialize();

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.removeStatements(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
	}
}
