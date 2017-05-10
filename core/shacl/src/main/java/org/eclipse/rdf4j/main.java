package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.SHACLSail;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by heshanjayasinghe on 4/30/17.
 */
public class Main {

    public static final String SH = "http://www.w3.org/ns/shacl#";

    public static void main(String[] args) {


        SailRepository shaclSail = new SailRepository(new SHACLSail(new MemoryStore()));
        shaclSail.initialize();

        try (SailRepositoryConnection conn = shaclSail.getConnection()) {
            String filename = "shacl.ttl";
            try (InputStream input = SHACLSail.class.getResourceAsStream("/" + filename)) {
                // add the RDF data from the inputstream directly to our database
                conn.add(input, "", RDFFormat.TURTLE );
            } catch (IOException e) {
                e.printStackTrace();
            }

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = conn.getStatements(null, null, null)) {
                while (result.hasNext()) {
                    Statement st = result.next();
                    System.out.println("db contains: " + st);
                }
            }
        }
        finally {
            shaclSail.shutDown();
        }


    }
}
