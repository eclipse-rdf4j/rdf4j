package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Shape {
    Resource id;
    List<PropertyShape> propertyShapes;
    TargetClass targetClass;


    public Shape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        propertyShapes = PropertyShape.Factory.getProprtyShapes(id,connection);
//        if(connection.hasStatement(id, SHACL.TARGET_CLASS, null, true)) {
//            targetClass = new TargetClass(id, connection);
//        }
    }

    public static class Factory {


        public static List<Shape> getShapes(SailRepositoryConnection connection) {
            List<Shape> shapes = new ArrayList<>();

            RepositoryResult<Statement> statements = connection.getStatements(null, RDF.TYPE, SHACL.SHAPE);
            while (statements.hasNext()) {
                Resource shapeId = statements.next().getSubject();
                if (hasTargetClass(shapeId, connection)) {
                    shapes.add(new TargetClass(shapeId, connection));
                } else {
                    shapes.add(new Shape(shapeId, connection));
                }
            }
            return shapes;
        }

        private static boolean hasTargetClass(Resource shapeId, SailRepositoryConnection connection) {
            if (connection.hasStatement(shapeId, SHACL.TARGET_CLASS, null, true)) return true;
            else return false;
        }
    }
    }
