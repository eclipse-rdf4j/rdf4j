/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.plan.*;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class MinCountPropertyShape extends PathPropertyShape  {

    public int minCount;
    Shape shape;

    public MinCountPropertyShape(Resource id, SailRepositoryConnection connection) {
        super(id,connection);
        try (RepositoryResult<Statement> statement = connection.getStatements(id, SHACL.MIN_COUNT, null, true)) {
                Literal object = (Literal) statement.next().getObject();
                minCount = object.intValue();
        }
    }

    @Override
    public String toString() {
        return "MinCountPropertyShape{" +
                "minCount=" + minCount +
                '}';
    }

    public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {

        PlanNode instancesOfTargetClass = shape.getPlan(shaclSailConnection,shape);
        PlanNode properties = super.getPlan(shaclSailConnection,shape);
        PlanNode join =  new OuterLeftJoin(instancesOfTargetClass, properties);
        GroupPlanNode groupBy = new GroupBy(join);
        return new MinCountValidator(groupBy, minCount);
    }

}
