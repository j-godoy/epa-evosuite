package org.evosuite.coverage.epa;

import java.io.IOException;
import java.io.Serializable;

import org.evosuite.testcase.execution.ExecutionResult;

/**
 * Represents a goal to be covered by the EPAExceptionCoverage criterion
 * 
 * @author jgaleotti
 */
public class EPAExceptionCoverageGoal implements Serializable, Comparable<EPAExceptionCoverageGoal> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4572794069479938065L;

	private final String className;
	private final EPAState fromState;
	private final String actionId;
	private final EPAState toState;

	public EPAExceptionCoverageGoal(String className, EPAState fromState, String actionId, EPAState toState) {
		this.className = className;
		this.fromState = fromState;
		this.actionId = actionId;
		this.toState = toState;
	}

	public String getMethodName() {
		String methodName;
		if (actionId.contains("(")) {
			methodName = actionId.split("\\(")[0];
		} else {
			methodName = actionId;
		}
		return methodName;
	}

	public String getClassName() {
		return className;
	}

	@Override
	public int compareTo(EPAExceptionCoverageGoal o) {
		String myKey = this.getGoalName();
		String otherKey = o.getGoalName();
		return myKey.compareTo(otherKey);
	}

	public String getGoalName() {
		return String.format("EPAException[%s,%s,%s]", fromState.getName(), actionId, toState.getName());
	}

	/**
	 * Returns 0.0 if the execution trace covers the transition, 1.0 otherwise If
	 * the execution trace has a INVALID_OBJECT_STATE, the rest of the trace is
	 * discarded.
	 * 
	 * @param result
	 * @return
	 */
	public double getDistance(ExecutionResult result) {
		for (EPATrace epa_trace : result.getTrace().getEPATraces()) {
			for (EPATransition epa_transition : epa_trace.getEpaTransitions()) {
				EPAState epa_transition_destination = epa_transition.getDestinationState();
				if (epa_transition_destination.equals(EPAState.INVALID_OBJECT_STATE)) {
					break;
				}
				
				String epa_transition_actionId = epa_transition.getActionName();
				if(epa_transition instanceof EPAExceptionalTransition) {
					epa_transition_actionId = EPAUtils.EXCEPTION_SUFFIX_ACTION_ID + epa_transition_actionId;
				}
				
				EPAState epa_transition_origin = epa_transition.getOriginState();
				if (epa_transition_origin.equals(this.fromState) && epa_transition_actionId.equals(this.actionId)
						&& epa_transition_destination.equals(this.toState)) {
					return 0.0;
				}
			}
		}
		return 1.0;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

	public String toString() {
		return getGoalName().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof EPAExceptionCoverageGoal) {
			EPAExceptionCoverageGoal other = (EPAExceptionCoverageGoal) obj;
			return this.getGoalName().equals(other.getGoalName());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getGoalName().hashCode();
	}

}
