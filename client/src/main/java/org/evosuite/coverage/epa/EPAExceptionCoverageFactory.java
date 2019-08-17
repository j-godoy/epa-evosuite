package org.evosuite.coverage.epa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.coverage.archive.TestsArchive;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractFitnessFactory;

public class EPAExceptionCoverageFactory extends AbstractFitnessFactory<EPAExceptionCoverageTestFitness> {
	
	public static int UPPER_BOUND_OF_GOALS;

	public EPAExceptionCoverageFactory(EPA epaAutomata) {
		// consider normal actions and exceptional actions
		int numberOfAutomataActions = epaAutomata.getActions().size() * 2;
		int maxNumberOfAutomataStates = epaAutomata.getStates().size();
		int maxNumberOfAutomataTransitions = (maxNumberOfAutomataStates * numberOfAutomataActions * maxNumberOfAutomataStates);
		UPPER_BOUND_OF_GOALS = maxNumberOfAutomataTransitions;
	}

	@Override
	public List<EPAExceptionCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAExceptionCoverageTestFitness>(goals.values());
	}
	
	private static Map<String, EPAExceptionCoverageTestFitness> goals = new LinkedHashMap<>();
	
	public static Map<String, EPAExceptionCoverageTestFitness> getGoals() {
        return goals;
    }
	
	public static Set<EPAExceptionCoverageTestFitness> calculateEPAExceptionInfo(List<ExecutionResult> results,
			EPAExceptionCoverageSuiteFitness contextFitness) {

		Set<EPAExceptionCoverageTestFitness> goalsCoveredByResults = new HashSet<>();

		for (ExecutionResult result : results) {
			for (EPATrace epa_trace : result.getTrace().getEPATraces()) {
				for (EPATransition epa_transition : epa_trace.getEpaTransitions()) {
					if (epa_transition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)) {
						break;
					}
					 
					String action_name = epa_transition.getActionName();
					if (epa_transition instanceof EPAExceptionalTransition) {
						action_name = EPAUtils.EXCEPTION_SUFFIX_ACTION_ID + action_name;
					}
					EPAExceptionCoverageGoal epaExceptionGoal = new EPAExceptionCoverageGoal(Properties.TARGET_CLASS, epa_transition.getOriginState(), action_name,
							epa_transition.getDestinationState());
					EPAExceptionCoverageTestFitness goal = new EPAExceptionCoverageTestFitness(epaExceptionGoal);
					
					if (!goalsCoveredByResults.contains(goal)) {
						goalsCoveredByResults.add(goal);
					}

					String key = goal.getGoalName();

					if (!EPAExceptionCoverageFactory.getGoals().containsKey(key)) {
						EPAExceptionCoverageFactory.getGoals().put(key, goal);
						if (Properties.TEST_ARCHIVE && contextFitness != null) {
							TestsArchive.instance.addGoalToCover(contextFitness, goal);
							TestsArchive.instance.putTest(contextFitness, goal, result);
						}
					}
				}
			}
		}

		return goalsCoveredByResults;
	}
	
}
