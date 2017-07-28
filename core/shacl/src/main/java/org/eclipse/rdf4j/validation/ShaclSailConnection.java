package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.AST.Shape;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper{

	public ShaclSail sail;

	public ShaclSailConnection(ShaclSail shaclSail, NotifyingSailConnection connection) {
		super(connection);
		this.sail = shaclSail;
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		super.begin(level);
	}

	@Override
	public void commit() throws SailException {
		super.commit();

		//sail.validate(this);

		for (Shape shape : sail.shapes) {
				shape.getPlan(this,shape);
		}
//            System.out.println(shape);
//            List<PlanNode> plans = shape.generatePlans(shaclSailConnection,shape);
//
//            for (PlanNode v : plans) {
//                System.out.println(v);
//                if(!v.validate()){//plan validate logic;
//                    throw new RuntimeException("Invalid repo");
//                }
//            }

	}

	protected Model createModel(){
		return new TreeModel();
	};

	public void validate(){
//		ShaclPlan plan = createPlan(sail.shapes,this); //normal plan ekak
//
//		plan.validate(this);
	}

}
