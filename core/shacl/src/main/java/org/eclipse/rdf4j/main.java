package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.SHACLConnection;
import org.eclipse.rdf4j.validation.SHACLSail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by heshanjayasinghe on 4/30/17.
 */
public class main {

    public static final String SH = "http://www.w3.org/ns/shacl#";

    public static void main(String[] args) {

        SailRepository shaclRules = new SailRepository(new MemoryStore());

        shaclRules.initialize();
        ValueFactory vf = shaclRules.getValueFactory();

        try (SailRepositoryConnection connection = shaclRules.getConnection()) {
            connection.begin();
            connection.add(new FileInputStream("shacl.ttl"), "", RDFFormat.TURTLE);
            connection.commit();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        SHACLSail shaclSail = new SHACLSail();

        shaclSail.setShaclRules(shaclRules);



        SHACLConnection connection = (SHACLConnection) shaclSail.getConnection();

//        connection.addShaclViolationListener(new ShaclViolationListener() {
//            @Override
//            public void violation(String errror) {
//                System.out.println(errror);
//            }
//        });

        connection.begin();


        connection.commit();


    }
}
