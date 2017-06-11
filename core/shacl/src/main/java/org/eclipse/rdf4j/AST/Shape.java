package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

import java.util.List;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Shape {
    Resource id;
    SailRepositoryConnection connection;
    List<PropertyShape> propertyShapes;
    TargetClass targetClass;


    public Shape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
        propertyShapes = new PropertyShape.Factory().getProprtyShapes(id,connection);
        ValueFactory vf = connection.getValueFactory();
        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "targetClass"), null, true)) {
            targetClass = new TargetClass(id, connection);
        }

    }

    static class Factory{
        Shape shape;
        Shape getShapes(Resource id, SailRepositoryConnection connection){
            if(hasShape()){
                new Shape(id ,connection);
            }
            return shape;
        }

        private boolean hasShape() {
            if (shape == null)
                return true;
            return false;
        }


    }



}
