package org.eclipse.rdf4j.shape;

import org.eclipse.rdf4j.Main;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class Shape {
    Resource id;
    SailRepositoryConnection connection;
    Resource targetClass;
    List<MinCountShape> shapes = new ArrayList<>();


    public Shape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
        ValueFactory vf = connection.getValueFactory();

        if(connection.hasStatement(id, vf.createIRI(Main.SH, "targetClass"), null, true)){
            targetClass = (Resource) connection.getStatements(id, vf.createIRI(Main.SH, "targetClass"), null).next().getObject();
        }

        RepositoryResult<Statement> property = connection.getStatements(id, vf.createIRI(Main.SH, "property"), null);
        while(property.hasNext()){
            Resource next = (Resource) property.next().getObject();

            if(connection.hasStatement(next, vf.createIRI(Main.SH, "minCount"), null, true)){
                shapes.add(new MinCountShape(next, connection));
            }
        }
    }

    @Override
    public String toString() {
        return "Shape{" +
                "id=" + id +
                ", connection=" + connection +
                ", targetClass=" + targetClass +
                '}';
    }


//    public boolean validate(List<Statement> addedStatement, MoreDataCacheLayer getMoreDataCacheLayer){
//        List<Boolean> collect = shapes.stream().map(shape -> shape.validate(addedStatement)).collect(Collectors.toList());
//
//        return false;
//    }
}
