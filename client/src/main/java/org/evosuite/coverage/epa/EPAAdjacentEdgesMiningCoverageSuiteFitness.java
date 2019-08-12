/**
 * Copyright (C) 2010-2016 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.epa;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.archive.TestsArchive;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPAAdjacentEdgesMiningCoverageSuiteFitness extends TestSuiteFitnessFunction {

	private static final long serialVersionUID = 1565793073526627496L;

	private static Logger logger = LoggerFactory.getLogger(EPAAdjacentEdgesMiningCoverageSuiteFitness.class);

	private static int maxEPAAdjacentEdgesMiningGoalsCovered = 0;

	public EPAAdjacentEdgesMiningCoverageSuiteFitness() {

	}

	@Override
	public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> suite) {
		logger.trace("Calculating EPAAdjacentPairs Mining fitness");

		Class<?> targetClass;
		try {
			targetClass = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(Properties.TARGET_CLASS);
		} catch (ClassNotFoundException e) {
			throw new EvosuiteError(e);
		}

		Map<String, Set<Constructor<?>>> actionConstructorMap = EPAUtils.getEpaActionConstructors(targetClass);
		Map<String, Set<Method>> actionMethodsMap = EPAUtils.getEpaActionMethods(targetClass);
		Map<String, Method> preconditionMethodsMap = EPAUtils.getEpaActionPreconditionMethods(targetClass);

		EPAUtils.checkActionAndPreconditionsAnnotationsForEpaMining(actionMethodsMap, preconditionMethodsMap);

		Set<String> actionIds = new HashSet<String>(actionMethodsMap.keySet());
		actionIds.addAll(actionConstructorMap.keySet());

		int numberOfAutomataActions = actionIds.size();
		int maxNumberOfAutomataStates = (int) Math.pow(2, numberOfAutomataActions);
		int maxNumberOfAutomataTransitions = maxNumberOfAutomataStates * numberOfAutomataActions * maxNumberOfAutomataStates;
		int maxNumOfDepartingEdges = numberOfAutomataActions * maxNumberOfAutomataStates;
		int totalGoals = (maxNumberOfAutomataTransitions * maxNumOfDepartingEdges) * 2;
		

		List<ExecutionResult> results = runTestSuite(suite);
		EPAAdjacentEdgesMiningCoverageSuiteFitness contextFitness = this;
		Set<EPAAdjacentEdgesMiningCoverageTestFitness> goalsCoveredByResult = calculateEPAAdjacentEdgesMiningInfo(results, contextFitness);

		if (Properties.TEST_ARCHIVE) {
			// If we are using the archive, then fitness is by definition 0
			// as all assertions already covered are in the archive
			suite.setFitness(this, 0.0);
			suite.setCoverage(this, 1.0);
			maxEPAAdjacentEdgesMiningGoalsCovered = EPAAdjacentEdgesMiningCoverageFactory.getGoals().size();
			return 0.0;
		}

		int numCoveredGoals = goalsCoveredByResult.size();
		int numUncoveredGoals = 0;
		for (EPAAdjacentEdgesMiningCoverageTestFitness knownCoverageGoal : EPAAdjacentEdgesMiningCoverageFactory.getGoals().values()) {
			if (!goalsCoveredByResult.contains(knownCoverageGoal)) {
				numUncoveredGoals++;
			}
		}

		if (numCoveredGoals > maxEPAAdjacentEdgesMiningGoalsCovered) {
			logger.info("(AdjacentPairs) Best individual covers " + numCoveredGoals + " transitions");
			maxEPAAdjacentEdgesMiningGoalsCovered = numCoveredGoals;
		}

		// We cannot set a coverage here, as it does not make any sense
		// suite.setCoverage(this, 1.0);
		double epaAdjacentEdgesMiningCoverageFitness = totalGoals - numCoveredGoals;

		suite.setFitness(this, epaAdjacentEdgesMiningCoverageFitness);
		if (maxEPAAdjacentEdgesMiningGoalsCovered > 0)
			suite.setCoverage(this, numCoveredGoals / maxEPAAdjacentEdgesMiningGoalsCovered);
		else
			suite.setCoverage(this, 1.0);

		suite.setNumOfCoveredGoals(this, numCoveredGoals);
		suite.setNumOfNotCoveredGoals(this, numUncoveredGoals);
		return epaAdjacentEdgesMiningCoverageFitness;
	}

	private static Set<EPAAdjacentEdgesMiningCoverageTestFitness> calculateEPAAdjacentEdgesMiningInfo(List<ExecutionResult> results,
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
