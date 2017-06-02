package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.ShaclAbstractSail;
import org.eclipse.rdf4j.validation.ShaclNotifyingSailConneectionBase;
import org.eclipse.rdf4j.validation.ShaclSail;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by heshanjayasinghe on 4/30/17.
 */
public class Main extends ShaclNotifyingSailConneectionBase {

    public static final String SH = "http://www.w3.org/ns/shacl#";

    //    public Main(SHACLSail shaclSail, NotifyingSailConnection connection) {
//        super(shaclSail, connection);
//    }

    public Main(AbstractSail abstractSail) {
        super(abstractSail);

    }


    public static void main(String[] args) {

        SailRepository sailrepo = new SailRepository(new ShaclSail(new MemoryStore()));
        ShaclAbstractSail abstractSail = new ShaclAbstractSail();
        abstractSail.initialize();
        Main mainInstance = new Main(abstractSail);
        mainInstance.addStatement(sailrepo);
       // mainInstance.removeStatement(sailrepo);
        //mainInstance.printStatements(sailrepo);

    }



    private void addStatement(SailRepository sailrepo){
        try (SailRepositoryConnection connection = sailrepo.getConnection()) {
            String filename = "shacl.ttl";
            try (InputStream input = ShaclSail.class.getResourceAsStream("/" + filename)) {
                connection.begin();

                ValueFactory valueFactory = connection.getValueFactory();
                notifyStatementAdded(valueFactory.createStatement(OWL.THING, RDFS.COMMENT, RDF.REST));
               // valueFactory.createStatement(OWL.THING, RDFS.COMMENT, RDF.REST);
                //connection.add(OWL.THING, RDFS.COMMENT, RDF.REST);

                connection.commit();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = connection.getStatements(null, null, null)) {
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

    private void removeStatement(SailRepository sailrepo){
        try (SailRepositoryConnection connection = sailrepo.getConnection()) {
            String filename = "shacl.ttl";
            try (InputStream input = ShaclSail.class.getResourceAsStream("/" + filename)) {
                connection.begin();

                ValueFactory valueFactory = connection.getValueFactory();
                notifyStatementRemoved(valueFactory.createStatement(OWL.THING, RDFS.COMMENT, RDF.REST));
                // valueFactory.createStatement(OWL.THING, RDFS.COMMENT, RDF.REST);
                //connection.add(OWL.THING, RDFS.COMMENT, RDF.REST);

                connection.commit();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = connection.getStatements(null, null, null)) {
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
            try (InputStream input = ShaclSail.class.getResourceAsStream("/" + filename)) {
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
