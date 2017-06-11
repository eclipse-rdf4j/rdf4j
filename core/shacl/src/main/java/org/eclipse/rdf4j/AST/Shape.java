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
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Shape {
    Resource id;
    SailRepositoryConnection connection;
    TargetClass targetClass;
    List<PropertyShape> propertyShapes = new ArrayList<>();


    public Shape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
        ValueFactory vf = connection.getValueFactory();

        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "targetClass"), null, true)){
            targetClass = (TargetClass) connection.getStatements(id, vf.createIRI(SH.BASE_URI, "targetClass"), null).next().getObject();
        }

        RepositoryResult<Statement> property = connection.getStatements(id, vf.createIRI(SH.BASE_URI, "property"), null);
        while(property.hasNext()){
            Resource next = (Resource) property.next().getObject();

            if(connection.hasStatement(next, vf.createIRI(SH.BASE_URI, "minCount"), null, true)){
                propertyShapes.add(new PropertyShape(next, connection));
            }
        }
    }

    static class Factory{

        List<Shape> getShapes(Resource propertyShapeId, SailRepositoryConnection connection){
            List<Shape> ret = new ArrayList<>();

            if(hasTargetClass(propertyShapeId, connection)){
                ret.add(new TargetClass(propertyShapeId, connection));
            }

            return ret;

        }

        private boolean hasTargetClass(Resource propertyShapeId, SailRepositoryConnection connection) {
            return true;
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

}
