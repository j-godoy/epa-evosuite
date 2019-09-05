package org.evosuite.coverage.epa;

import java.math.BigInteger;
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

public class EPAAdjacentEdgesMiningCoverageFactory extends AbstractFitnessFactory<EPAAdjacentEdgesCoverageTestFitness> {

    private static Map<String, EPAAdjacentEdgesCoverageTestFitness> goals = new LinkedHashMap<>();
    
    public static BigInteger UPPER_BOUND_OF_GOALS;

    public EPAAdjacentEdgesMiningCoverageFactory() {
    	int numberOfAutomataActions = EPAUtils.checkActionAndPreconditionsAnnotationsForMiningAndgetActionsSize();
		long maxNumberOfAutomataStates = (int) Math.pow(2, numberOfAutomataActions);
		BigInteger maxNumberOfAutomataTransitions = BigInteger.valueOf(maxNumberOfAutomataStates).multiply(BigInteger.valueOf(numberOfAutomataActions)).multiply(BigInteger.valueOf(maxNumberOfAutomataStates));
		BigInteger maxNumOfDepartingEdges = BigInteger.valueOf(numberOfAutomataActions).multiply(BigInteger.valueOf(maxNumberOfAutomataStates));
		UPPER_BOUND_OF_GOALS = maxNumberOfAutomataTransitions.multiply(maxNumOfDepartingEdges).multiply(BigInteger.valueOf(2));
	}

    public static Map<String, EPAAdjacentEdgesCoverageTestFitness> getGoals() {
        return goals;
    }

    /** {@inheritDoc} */
	@Override
	public List<EPAAdjacentEdgesCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAAdjacentEdgesCoverageTestFitness>(goals.values());
	}
	
	public static Set<EPAAdjacentEdgesCoverageTestFitness> calculateEPAAdjacentEdgesMiningInfo(List<ExecutionResult> results,
			EPAAdjacentEdgesMiningCoverageSuiteFitness contextFitness) {

		Set<EPAAdjacentEdgesCoverageTestFitness> goalsCoveredByResults = new HashSet<>();

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
				EPAAdjacentEdgesCoverageGoal g = new EPAAdjacentEdgesCoverageGoal(Properties.TARGET_CLASS,
						firstTransition, secondTransition);
				EPAAdjacentEdgesCoverageTestFitness goal = new EPAAdjacentEdgesCoverageTestFitness(g);
				
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
