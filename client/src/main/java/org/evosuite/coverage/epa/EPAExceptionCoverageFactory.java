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
	
	public static long UPPER_BOUND_OF_GOALS;
	private static EPA EPA;

	public EPAExceptionCoverageFactory(EPA epaAutomata) {
		// consider normal actions and exceptional actions
		int numberOfAutomataActions = epaAutomata.getActions().size();//exceptional actions
		int maxNumberOfAutomataStates = epaAutomata.getStates().size();
		long maxNumberOfAutomataTransitions = (maxNumberOfAutomataStates * numberOfAutomataActions * maxNumberOfAutomataStates);
		UPPER_BOUND_OF_GOALS = maxNumberOfAutomataTransitions + epaAutomata.getTransitions().size();
		EPA = epaAutomata;
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
					
					if(epa_transition instanceof EPANormalTransition && !EPA.getTransitions().contains(epa_transition))
						break; // only consider normal actions in the EPA automata

					String action_name = epa_transition.getActionName();
					EPAExceptionCoverageGoal epaExceptionGoal = new EPAExceptionCoverageGoal(Properties.TARGET_CLASS, epa_transition.getOriginState(),
							action_name, epa_transition.getDestinationState());
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
