package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.main;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.shape.Shape;


import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class SHACLSail extends NotifyingSailWrapper {

    List<Shape> shapes;

    @Override
    public void setBaseSail(Sail baseSail) {
        super.setBaseSail(baseSail);
    }

    @Override
    public NotifyingSailConnection getConnection() throws SailException {
        return new SHACLConnection(this);
    }

    public void setShaclRules(SailRepository shaclRules){
        try(SailRepositoryConnection connection = shaclRules.getConnection()){
            ValueFactory vf = connection.getValueFactory();
            RepositoryResult<Statement> nodeShape = connection.getStatements(null, RDF.TYPE,vf.createIRI(main.SH,"NodeShape"));
            List<Resource> collect = Iterations.stream(nodeShape).map(Statement::getSubject).collect(Collectors.toList());

            collect.forEach(System.out::println);
            shapes = collect.stream().map(s -> new Shape(s, connection)).collect(Collectors.toList());

        }

    }

}
