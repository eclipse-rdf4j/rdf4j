package org.eclipse.rdf4j.spring.dao.support.opbuilder;

import static org.eclipse.rdf4j.spring.dao.support.operation.OperationUtils.setBindings;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.spring.dao.support.UpdateCallback;
import org.eclipse.rdf4j.spring.support.Rdf4JTemplate;

public class UpdateExecutionBuilder extends OperationBuilder<Update, UpdateExecutionBuilder> {

	public UpdateExecutionBuilder(Update update, Rdf4JTemplate template) {
		super(update, template);
	}

	public void execute() {
		Update update = getOperation();
		setBindings(update, getBindings());
		update.execute();
	}

	public void execute(UpdateCallback updateCallback) {
		Map<String, Value> bindings = getBindings();
		Update update = getOperation();
		setBindings(update, bindings);
		update.execute();
		updateCallback.accept(bindings);
	}
}
