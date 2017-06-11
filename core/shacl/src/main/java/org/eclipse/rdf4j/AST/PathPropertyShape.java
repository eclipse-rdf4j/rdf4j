package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/11/17.
 */
public class PathPropertyShape extends PropertyShape {
    List<Path> paths = new ArrayList<>();

    public PathPropertyShape(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
        this.id = id;
        this.connection = connection;
        ValueFactory vf = connection.getValueFactory();

        RepositoryResult<Statement> property = connection.getStatements(id, vf.createIRI(SH.BASE_URI, "property"), null);

        while(property.hasNext()){
            Resource resourcenext = (Resource) property.next().getObject();

            if(connection.hasStatement(resourcenext, vf.createIRI(SH.BASE_URI, "path"), null, true)){
                paths.add(new Path(resourcenext, connection));
            }
        }
    }

    public List<Path> getPaths() {
        return paths;
    }
}

