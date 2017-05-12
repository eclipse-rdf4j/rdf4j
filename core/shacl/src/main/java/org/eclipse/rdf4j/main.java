package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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


        SailRepository sailrepo = new SailRepository(new SHACLSail(new MemoryStore()));
        sailrepo.initialize();
        Main mainInstance = new Main();
        mainInstance.addStatement(sailrepo);
       // mainInstance.printStatements(sailrepo);

    }

    private void addStatement(SailRepository sailrepo){
        try (SailRepositoryConnection conn = sailrepo.getConnection()) {
            String filename = "shacl.ttl";
            try (InputStream input = SHACLSail.class.getResourceAsStream("/" + filename)) {
//                ValueFactory factory = SimpleValueFactory.getInstance();
//                IRI bob = factory.createIRI("http://example.org/bob");
//                IRI name = factory.createIRI("http://example.org/name");
//                Literal bobsName = factory.createLiteral("Bob");
//                Statement nameStatement = factory.createStatement(bob, name, bobsName);
//                new SHACLSailConnection((SHACLSail) sailrepo.getSail(), (NotifyingSailConnection) sailrepo.getSail().getConnection()).statementAdded(nameStatement);
              //  addInferredStatement(OWL.THING, RDFS.COMMENT, RDF.REST);
               // new SHACLSailConnection((SHACLSail) sailrepo.getSail(), (NotifyingSailConnection) sailrepo.getSail().getConnection()).statementAdded(nameStatement)
 //               addStatement(sailrepo.getSail().getValueFactory().createStatement(OWL.THING, RDFS.COMMENT, RDF.REST));
                conn.begin();
                conn.getSailConnection().addStatement(OWL.THING, RDFS.COMMENT, RDF.REST);
                conn.commit();
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
            sailrepo.shutDown();
        }

    }


    private void printStatements(SailRepository sailrepo){
        try (SailRepositoryConnection conn = sailrepo.getConnection()) {
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
            sailrepo.shutDown();
        }
    }
}
