package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PatternFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;

public class PatternConstraintComponent extends SimpleAbstractConstraintComponent {

	String pattern;
	String flags;

	public PatternConstraintComponent(String pattern, String flags) {
		this.pattern = pattern;
		this.flags = flags;

		if (flags == null) {
			this.flags = "";
		}
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.PATTERN, SimpleValueFactory.getInstance().createLiteral(pattern));
		if (flags != null && !flags.isEmpty()) {
			model.add(subject, SHACL.FLAGS, SimpleValueFactory.getInstance().createLiteral(flags));
		}

	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return "!isBlank(?" + varName + ") && REGEX(STR(?" + varName + "), \"" + escapeRegexForSparql(pattern)
					+ "\", \"" + flags + "\") ";
		} else {
			return " isBlank(?" + varName + ") || !REGEX(STR(?" + varName + "), \"" + escapeRegexForSparql(pattern)
					+ "\", \"" + flags + "\") ";
		}
	}

	private static String escapeRegexForSparql(String pattern) {
		return pattern.replace("\\", "\\\\");
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.PatternConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new PatternConstraintComponent(pattern, flags);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new PatternFilter(parent, pattern, flags);
	}
}
