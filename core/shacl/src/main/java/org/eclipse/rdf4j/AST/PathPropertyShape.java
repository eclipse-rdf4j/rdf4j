package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

/**
 * Created by heshanjayasinghe on 6/11/17.
 */
public class PathPropertyShape extends PropertyShape {
    public Path path;

    public PathPropertyShape(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
        this.id = id;
        this.connection = connection;
        ValueFactory vf = connection.getValueFactory();



        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "path"), null, true)) {
            path = new Path(id, connection);
        }

    }


}

