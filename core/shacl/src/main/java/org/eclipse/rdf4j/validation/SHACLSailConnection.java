package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class SHACLSailConnection extends NotifyingSailConnectionWrapper
		implements SailConnectionListener {

	private SHACLSail sail;
	private boolean statementsRemoved;
	private Model newStatements;



	public SHACLSailConnection(SHACLSail shaclSail, NotifyingSailConnection connection) {
		super(connection);
		this.sail = shaclSail;

	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		super.begin(level);
	}

	@Override
	public void statementAdded(Statement statement) {
		if (statementsRemoved) {
			return;
		}

		if (newStatements == null) {
			newStatements = createModel();
		}
		newStatements.add(statement);
	}


	@Override
	public void statementRemoved(Statement statement) {
		boolean removed = (newStatements != null) ? newStatements.remove(statement) : false;
		if (!removed) {
			statementsRemoved = true;
			newStatements = null;
		}
	}



	@Override
	public void commit() throws SailException {
		super.commit();

	}

	protected Model createModel(){
		return new TreeModel();
	};


}
