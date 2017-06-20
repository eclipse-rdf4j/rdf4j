package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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
    List<PropertyShape> propertyShapes;
    TargetClass targetClass;


    public Shape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        propertyShapes = new PropertyShape.Factory().getProprtyShapes(id,connection);
        ValueFactory vf = connection.getValueFactory();
        if(connection.hasStatement(id, vf.createIRI(SH.BASE_URI, "targetClass"), null, true)) {
            targetClass = new TargetClass(id, connection);
        }
    }

    public static class Factory{

        private  List<Shape> shapes;
        public  List<Shape> getShapes(SailRepositoryConnection connection) {
            shapes = new ArrayList<>();

//SHACL.SHAPE --> RDFS.RESOURCE
            RepositoryResult<Statement> statements = connection.getStatements(null, RDF.TYPE, SHACL.SHAPE);
            while (statements.hasNext()) {
                Resource shapeId = statements.next().getSubject();
                if (hasTargetClass(shapeId, connection)) {
                    shapes.add(new TargetClass(shapeId, connection));
                } else {
                    shapes.add(new Shape(shapeId, connection));
                }
                return shapes;
            }
            return shapes;
        }

        private boolean hasTargetClass(Resource shapeId, SailRepositoryConnection connection) {
            for (Shape shape:shapes) {
                if(shape instanceof TargetClass)
                    return true;
            }
            return false;
        }
        }

    }
