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

public class EPAAdjacentEdgesCoverageFactory extends AbstractFitnessFactory<EPAAdjacentEdgesCoverageTestFitness> {

	public static long UPPER_BOUND_OF_GOALS;

	public EPAAdjacentEdgesCoverageFactory(EPA epaAutomata) {
		int numOfStates = epaAutomata.getStates().size();
		int numOfActions = epaAutomata.getActions().size();
		int maxNumOfEdges = numOfStates * numOfActions * numOfStates;
		int maxNumOfDepartingEdges = numOfActions * numOfStates;
		UPPER_BOUND_OF_GOALS = (maxNumOfEdges * maxNumOfDepartingEdges) * 2;
	}

	private static Map<String, EPAAdjacentEdgesCoverageTestFitness> goals = new LinkedHashMap<>();

	public static Map<String, EPAAdjacentEdgesCoverageTestFitness> getGoals() {
		return goals;
	}

	@Override
	public List<EPAAdjacentEdgesCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAAdjacentEdgesCoverageTestFitness>(goals.values());
	}
	
	public static Set<EPAAdjacentEdgesCoverageTestFitness> calculateEPAAdjacentEdgesInfo(List<ExecutionResult> results,
			EPAAdjacentEdgesCoverageSuiteFitness contextFitness) {

		Set<EPAAdjacentEdgesCoverageTestFitness> goalsCoveredByResults = new HashSet<>();

		for (ExecutionResult result : results) {
			Set<EPAAdjacentEdgesPair> adjacentPairsForResult = EPAAdjacentEdgesPair.getAdjacentEdgesPairsExecuted(result);
			for (EPAAdjacentEdgesPair pair : adjacentPairsForResult) {
				EPATransition firstTransition = pair.getFirstEpaTransition();
				EPATransition secondTransition = pair.getSecondEpaTransition();
				EPAAdjacentEdgesCoverageGoal edgesGoal = new EPAAdjacentEdgesCoverageGoal(Properties.TARGET_CLASS, firstTransition, secondTransition);
				EPAAdjacentEdgesCoverageTestFitness goal = new EPAAdjacentEdgesCoverageTestFitness(edgesGoal);
				
				if (!goalsCoveredByResults.contains(goal)) {
					goalsCoveredByResults.add(goal);
				}

				String key = goal.getKey();

				if (!EPAAdjacentEdgesCoverageFactory.getGoals().containsKey(key)) {
					EPAAdjacentEdgesCoverageFactory.getGoals().put(key, goal);
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
