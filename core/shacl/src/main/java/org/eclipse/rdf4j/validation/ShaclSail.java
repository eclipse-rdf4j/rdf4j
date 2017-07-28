package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.AST.PropertyShape;
import org.eclipse.rdf4j.AST.Shape;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class ShaclSail extends NotifyingSailWrapper {

    public List<Shape> shapes;
    public List<PropertyShape> propertyShapes;
    public Model newStatements;
    private boolean statementsRemoved;
    private SailRepository shacl;

    public ShaclSail(NotifyingSail memoryStore) {
        super(memoryStore);
    }

    public ShaclSail(MemoryStore memoryStore,SailRepository shacl) {
        super(memoryStore);
        this.shacl = shacl;
    }

    public void initialize()throws SailException{
        super.initialize();

        try(SailRepositoryConnection connection = shacl.getConnection()){
            shapes = Shape.Factory.getShapes(connection);
        }
    }

    @Override
    public NotifyingSailConnection getConnection() throws SailException {
        try {
            NotifyingSailConnection con = super.getConnection();
            ShaclSailConnection shaclSailConnection = new ShaclSailConnection(this,con);
            shaclSailConnection.addConnectionListener(new SailConnectionListener() {

                @Override
                public void statementAdded(Statement statement) {
                    if (statementsRemoved) {
                        return;
                    }
                    if (newStatements == null) {
                        newStatements = createModel();
                    }
                    newStatements.add(statement);
                    System.out.println("statement added : "+statement);
                }

                private Model createModel() {
                    return new TreeModel();
                }

                @Override
                public void statementRemoved(Statement statement) {
                    boolean removed = (newStatements != null) ? newStatements.remove(statement) : false;
                    if (!removed) {
                        statementsRemoved = true;
                        newStatements = null;
                    }
                    System.out.println("statement removed : "+statement);
                }
            }

            );
            return shaclSailConnection;
        }
        catch (ClassCastException e) {
            throw new SailException(e.getMessage(), e);
        }
    }
    


    public void setShaclRules(SailRepository shaclRules){
        try(SailRepositoryConnection connection = shaclRules.getConnection()){
            ValueFactory vf = connection.getValueFactory();
            RepositoryResult<Statement> nodeShape = connection.getStatements(null, RDF.TYPE,SHACL.NODE_SHAPE);
            List<Resource> collect = Iterations.stream(nodeShape).map(Statement::getSubject).collect(Collectors.toList());

            collect.forEach(System.out::println);
            shapes = collect.stream().map(s -> new Shape(s, connection)).collect(Collectors.toList());

        }

    }


    public void validate(ShaclSailConnection shaclSailConnection) {

//        for (Shape shape : shapes) {
//            System.out.println(shape);
//            List<PlanNode> plans = shape.generatePlans(shaclSailConnection,shape);
//
//            for (PlanNode v : plans) {
//                System.out.println(v);
//                if(!v.validate()){//plan validate logic;
//                    throw new RuntimeException("Invalid repo");
//                }
//            }
//
//        }


    }

}
