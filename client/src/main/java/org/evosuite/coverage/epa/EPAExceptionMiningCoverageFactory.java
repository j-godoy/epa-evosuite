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

public class EPAExceptionMiningCoverageFactory extends AbstractFitnessFactory<EPAExceptionMiningCoverageTestFitness> {

    private static Map<String, EPAExceptionMiningCoverageTestFitness> goals = new LinkedHashMap<>();
    
    public EPAExceptionMiningCoverageFactory() {
    	EPAUtils.checkActionAndPreconditionsAnnotationsForMiningAndgetActionsSize();
	}

    public static Map<String, EPAExceptionMiningCoverageTestFitness> getGoals() {
        return goals;
    }

    /** {@inheritDoc} */
	@Override
	public List<EPAExceptionMiningCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAExceptionMiningCoverageTestFitness>(goals.values());
	}
	
	public static Set<EPAExceptionMiningCoverageTestFitness> calculateEPAExceptionMiningInfo(List<ExecutionResult> results,
			EPAExceptionMiningCoverageSuiteFitness contextFitness) {

		Set<EPAExceptionMiningCoverageTestFitness> goalsCoveredByResults = new HashSet<>();

		for (ExecutionResult result : results) {
			for (EPATrace epa_trace : result.getTrace().getEPATraces()) {
				for (EPATransition epa_transition : epa_trace.getEpaTransitions()) {
					if (epa_transition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)) {
						break;
					}
					 
					String action_name = epa_transition.getActionName();
					
					EPAExceptionMiningCoverageTestFitness goal = new EPAExceptionMiningCoverageTestFitness(Properties.TARGET_CLASS,
							epa_transition.getOriginState(), action_name,
							epa_transition.getDestinationState());

					if (!goalsCoveredByResults.contains(goal)) {
						goalsCoveredByResults.add(goal);
					}

					String key = goal.getKey();

					if (!EPAExceptionMiningCoverageFactory.getGoals().containsKey(key)) {
						EPAExceptionMiningCoverageFactory.getGoals().put(key, goal);
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
