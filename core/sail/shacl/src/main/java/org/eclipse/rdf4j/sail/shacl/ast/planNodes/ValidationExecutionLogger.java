package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationExecutionLogger {

	private final static Logger logger = LoggerFactory.getLogger(ValidationExecutionLogger.class);

	private List<LogStatement> list = null;

	private static final boolean groupedLogging = true;

	void log(int depth, String name, ValidationTuple tuple, PlanNode planNode, String id, String message) {
		LogStatement logStatement = new LogStatement(depth, name, tuple, planNode, id, message);
		if (groupedLogging) {
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(logStatement);
		} else {
			logger.info(logStatement.toString());
		}
	}

	public void flush() {

		if (list == null || list.isEmpty()) {
			return;
		}

		Map<String, List<LogStatement>> map = new HashMap<>();

		list.forEach(s -> {
			map.computeIfAbsent(s.getId(), (ss) -> new ArrayList<>());
			map.get(s.getId()).add(s);
		});

		Set<String> printed = new HashSet<>();

		for (LogStatement logStatement : list) {
			if (!printed.contains(logStatement.getId())) {
				printed.add(logStatement.getId());
				map.get(logStatement.getId()).stream().map(Object::toString).forEachOrdered(logger::info);
			}
		}
	}

}

class LogStatement {

	private final int depth;
	private final String name;
	private final ValidationTuple tuple;
	private final PlanNode planNode;
	private final String id;
	private final String message;

	LogStatement(int depth, String name, ValidationTuple tuple, PlanNode planNode, String id, String message) {
		this.depth = depth;
		this.name = name;
		this.tuple = tuple;
		assert tuple != null;
		this.planNode = planNode;
		this.id = id;
		this.message = message;
	}

	public int getDepth() {
		return depth;
	}

	public String getName() {
		return name;
	}

	public ValidationTuple getTuple() {
		return tuple;
	}

	public PlanNode getPlanNode() {
		return planNode;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {

		return StringUtils.leftPad(id, 14) + "\t"
				+ leadingSpace(depth) + name
				+ ":  " + tuple.toString()
				+ " :  " + planNode.toString()
				+ (message != null ? " :  " + message : "");

	}

	private static String leadingSpace(int depth) {
		StringBuilder builder = new StringBuilder();
		for (int i = depth; i > 0; i--) {
			builder.append("  ");
		}
		return builder.toString();
	}

}
