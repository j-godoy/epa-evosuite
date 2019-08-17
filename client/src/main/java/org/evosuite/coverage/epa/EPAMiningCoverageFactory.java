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

public class EPAMiningCoverageFactory extends AbstractFitnessFactory<EPAMiningCoverageTestFitness> {

    private static Map<String, EPAMiningCoverageTestFitness> goals = new LinkedHashMap<>();
    
    public static int UPPER_BOUND_OF_GOALS;

    public EPAMiningCoverageFactory() {
    	int numberOfAutomataActions = EPAUtils.checkActionAndPreconditionsAnnotationsForMiningAndgetActionsSize();
    	int maxNumberOfAutomataStates = (int) Math.pow(2, numberOfAutomataActions);
		int maxNumberOfAutomataTransitions = maxNumberOfAutomataStates * numberOfAutomataActions * maxNumberOfAutomataStates;
		UPPER_BOUND_OF_GOALS = maxNumberOfAutomataTransitions;
	}

    public static Map<String, EPAMiningCoverageTestFitness> getGoals() {
        return goals;
    }

    /** {@inheritDoc} */
	@Override
	public List<EPAMiningCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAMiningCoverageTestFitness>(goals.values());
	}
	
	public static Set<EPAMiningCoverageTestFitness> calculateEPAMiningInfo(List<ExecutionResult> results,
			EPAMiningCoverageSuiteFitness contextFitness) {

		Set<EPAMiningCoverageTestFitness> goalsCoveredByResults = new HashSet<EPAMiningCoverageTestFitness>();

		for (ExecutionResult result : results) {
			for (EPATrace epa_trace : result.getTrace().getEPATraces()) {
				for (EPATransition epa_transition : epa_trace.getEpaTransitions()) {
					if (epa_transition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)) {
						break;
					}
					if (epa_transition instanceof EPAExceptionalTransition) {
						break;
					}

					EPAMiningCoverageTestFitness goal = new EPAMiningCoverageTestFitness(Properties.TARGET_CLASS,
							epa_transition.getOriginState(), epa_transition.getActionName(),
							epa_transition.getDestinationState());

					if (!goalsCoveredByResults.contains(goal)) {
						goalsCoveredByResults.add(goal);
					}

					String key = goal.getKey();

					if (!EPAMiningCoverageFactory.getGoals().containsKey(key)) {
						EPAMiningCoverageFactory.getGoals().put(key, goal);
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
