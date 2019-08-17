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

public class EPAAdjacentEdgesMiningCoverageFactory extends AbstractFitnessFactory<EPAAdjacentEdgesMiningCoverageTestFitness> {

    private static Map<String, EPAAdjacentEdgesMiningCoverageTestFitness> goals = new LinkedHashMap<>();
    
    public static int UPPER_BOUND_OF_GOALS;

    public EPAAdjacentEdgesMiningCoverageFactory() {
    	int numberOfAutomataActions = EPAUtils.checkActionAndPreconditionsAnnotationsForMiningAndgetActionsSize();
		int maxNumberOfAutomataStates = (int) Math.pow(2, numberOfAutomataActions);
		int maxNumberOfAutomataTransitions = maxNumberOfAutomataStates * numberOfAutomataActions * maxNumberOfAutomataStates;
		int maxNumOfDepartingEdges = numberOfAutomataActions * maxNumberOfAutomataStates;
		UPPER_BOUND_OF_GOALS = (maxNumberOfAutomataTransitions * maxNumOfDepartingEdges) * 2;
	}

    public static Map<String, EPAAdjacentEdgesMiningCoverageTestFitness> getGoals() {
        return goals;
    }

    /** {@inheritDoc} */
	@Override
	public List<EPAAdjacentEdgesMiningCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAAdjacentEdgesMiningCoverageTestFitness>(goals.values());
	}
	
	public static Set<EPAAdjacentEdgesMiningCoverageTestFitness> calculateEPAAdjacentEdgesMiningInfo(List<ExecutionResult> results,
			EPAAdjacentEdgesMiningCoverageSuiteFitness contextFitness) {

		Set<EPAAdjacentEdgesMiningCoverageTestFitness> goalsCoveredByResults = new HashSet<>();

		for (ExecutionResult result : results) {
			
			Set<EPAAdjacentEdgesPair> adjacentPairsForResult = EPAAdjacentEdgesPair.getAdjacentEdgesPairsExecuted(result);
			for (EPAAdjacentEdgesPair pair : adjacentPairsForResult) {
				EPATransition firstTransition = pair.getFirstEpaTransition();
				EPATransition secondTransition = pair.getSecondEpaTransition();
				
				if (firstTransition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)
						|| secondTransition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)) {
					// discard the rest of the trace if an invalid object state is reached
					break;
				}
				
				EPAAdjacentEdgesMiningCoverageTestFitness goal = new EPAAdjacentEdgesMiningCoverageTestFitness(Properties.TARGET_CLASS,
						firstTransition, secondTransition);
				
				if (!goalsCoveredByResults.contains(goal)) {
					goalsCoveredByResults.add(goal);
				}

				String key = goal.getKey();

				if (!EPAAdjacentEdgesMiningCoverageFactory.getGoals().containsKey(key)) {
					EPAAdjacentEdgesMiningCoverageFactory.getGoals().put(key, goal);
					if (Properties.TEST_ARCHIVE && contextFitness != null) {
						TestsArchive.instance.addGoalToCover(contextFitness, goal);
						TestsArchive.instance.putTest(contextFitness, goal, result);
					}
				}
			}
		}

		return goalsCoveredByResults;
	}
}
