package org.eclipse.rdf4j.validation;

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

	private ShaclSail sail;

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

		sail.validate(this);

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
