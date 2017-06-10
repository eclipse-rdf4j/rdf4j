package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Shape implements ExNode {
    Resource id;
    SailRepositoryConnection connection;
    TargetClass targetClass;
    List<PropertyShape> property = new ArrayList<>();


    public Shape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
        ValueFactory vf = connection.getValueFactory();

        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "targetClass"), null, true)){
            targetClass = (TargetClass) connection.getStatements(id, vf.createIRI(SH.BASE_URI, "targetClass"), null).next().getObject();
        }

//        RepositoryResult<Statement> property = connection.getStatements(id, vf.createIRI(SH.BASE_URI, "property"), null);
//        while(property.hasNext()){
//            Resource next = (Resource) property.next().getObject();
//
//            if(connection.hasStatement(next, vf.createIRI(SH.BASE_URI, "minCount"), null, true)){
//                shapes.add(new MinCountShape(next, connection));
//            }
//        }
    }

    @Override
    public String toString() {
        return "Shape{" +
                "id=" + id +
                ", connection=" + connection +
                ", targetClass=" + targetClass +
                '}';
    }

}
