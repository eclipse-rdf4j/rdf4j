/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

/**
 * Created by heshanjayasinghe on 7/18/17.
 */
public class Select implements PlanNode {

    public ShaclSailConnection shaclSailConnection;
    Resource type;
    Resource subject;
    IRI predicate;
    Value object;

    public Select(ShaclSailConnection shaclSailConnection, Resource type) {
        this.shaclSailConnection =shaclSailConnection;
        this.type=type;
    }

    public Select(ShaclSailConnection shaclSailConnection,Resource subject, IRI predicate, Value object) {
        this.shaclSailConnection =shaclSailConnection;
        this.subject =subject;
        this.predicate=predicate;
        this.object=object;
    }

    @Override
    public CloseableIteration<Tuple,SailException> iterator(){
        return new CloseableIteration<Tuple,SailException>() {

            CloseableIteration<? extends Statement, SailException> statements = shaclSailConnection.getStatements(subject, predicate, object, false);

            @Override
            public void close() throws SailException {
                statements.close();
            }

            int counter = 0;

            @Override
            public boolean hasNext() {
               return statements.hasNext();
            }

            @Override
            public Tuple next() {

                Statement next = statements.next();
                Tuple tuple = new Tuple();
                tuple.line.add(next.getSubject());
                tuple.line.add(next.getPredicate());
                tuple.line.add(next.getObject());

               counter++;

                return tuple;
            }

            @Override
            public void remove() throws SailException {

            }
        };
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public int getCardinalityMin() {
        return 3;
    }

    @Override
    public int getCardinalityMax() {
        return 3;
    }
}
