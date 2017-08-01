package org.eclipse.rdf4j.AST;


import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.plan.*;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

import java.util.Iterator;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class MinCountPropertyShape extends PathPropertyShape implements Iterable<Tuple> {

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


    public Select getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
        //super.getPlan(shaclSailConnection, shape);
        PlanNode instancesOfTargetClass = shape.getPlan(shaclSailConnection,shape);
        PlanNode properties = super.getPlan(shaclSailConnection,shape);

        Tuple targetclasstuple = instancesOfTargetClass.iterator().next();
        Tuple propertyTuple = ((Select) properties).shaclSailConnection.sail.newStatements;
        PlanNode join =  new OuterLeftJoin(instancesOfTargetClass, properties); //condition

     //   PlanNode groupBy = new GroupBy(join, condition); //condition

     //   PlanNode count = new Count(groupBy); //condition


//        Select validate = ValidateMinCount(count, minCount);
//        return validate;
        return null;
    }

//    private Select ValidateMinCount(PlanNode count, int minCount) {
//        PlanNode minCountValidator = new MinCountValidator(count,minCount);
//        minCountValidator.validate();
//       // return new Select(count);
//        return  new Select(count);
//    }


    @Override
    public Iterator<Tuple> iterator() {
        return null;
    }



}
