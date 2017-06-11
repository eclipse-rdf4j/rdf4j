package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class TargetClass extends Shape {
    Resource id;
    SailRepositoryConnection connection;
    Resource targetClass;

    public TargetClass(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
        this.id = id;
        this.connection = connection;
        ValueFactory vf = connection.getValueFactory();

        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "targetClass"), null, true)){
            targetClass = (Resource) connection.getStatements(id, vf.createIRI(SH.BASE_URI, "targetClass"), null).next().getObject();
        }
    }
}
