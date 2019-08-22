package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValidationExecutionLogger {

	private final static Logger logger = LoggerFactory.getLogger(ValidationExecutionLogger.class);

	private List<LogStatement> list = new ArrayList<>();

	private static boolean groupedLogging = true;

	void log(int depth, String name, Tuple tuple, PlanNode planNode, String id) {
		LogStatement logStatement = new LogStatement(depth, name, tuple, planNode, id);
		if (groupedLogging) {
			list.add(logStatement);
		} else {
			logger.info(logStatement.toString());
		}
	}

	public void flush() {

		if (list.isEmpty()) {
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
	private final Tuple tuple;
	private final PlanNode planNode;
	private final String id;

	LogStatement(int depth, String name, Tuple tuple, PlanNode planNode, String id) {
		this.depth = depth;
		this.name = name;
		this.tuple = tuple;
		this.planNode = planNode;
		this.id = id;
	}

	public int getDepth() {
		return depth;
	}

	public String getName() {
		return name;
	}

	public Tuple getTuple() {
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
				+ " :  " + planNode.toString();

	}

	private static String leadingSpace(int depth) {
		StringBuilder builder = new StringBuilder();
		for (int i = depth; i > 0; i--) {
			builder.append("  ");
		}
		return builder.toString();
	}

}
