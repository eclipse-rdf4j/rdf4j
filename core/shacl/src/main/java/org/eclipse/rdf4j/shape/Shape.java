package org.eclipse.rdf4j.shape;

import org.eclipse.rdf4j.main;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class Shape {
    Resource id;
    SailRepositoryConnection connection;
    Resource targetClass;


    public Shape(Resource id, SailRepositoryConnection connection) {
        ValueFactory vf = connection.getValueFactory();
        if(connection.hasStatement(id, vf.createIRI(main.SH, "targetClass"), null, true)){
            targetClass = (Resource) connection.getStatements(id, vf.createIRI(main.SH, "targetClass"), null).next().getObject();
        }
    }
}
