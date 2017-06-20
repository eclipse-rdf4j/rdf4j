package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Path implements Resource {
    public Resource path;
    Resource id;
    SailRepositoryConnection connection;

    public Path(Resource next, SailRepositoryConnection connection) {
        super();
        this.id = id;
        this.connection = connection;

        ValueFactory vf = connection.getValueFactory();
        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "path"), null, true)) {
            path = (Resource) connection.getStatements(next, vf.createIRI(SH.BASE_URI, "path"), null, true).next().getObject();
        }
    }

    @Override
    public String toString() {
        return "Path{" +
                "path=" + path +
                '}';
    }

    @Override
    public String stringValue() {
        return null;
    }
}
