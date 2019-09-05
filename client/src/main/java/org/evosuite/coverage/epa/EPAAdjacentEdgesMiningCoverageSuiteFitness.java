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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.testcase.ExecutableChromosome;
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

		List<ExecutionResult> results = runTestSuite(suite);
		EPAAdjacentEdgesMiningCoverageSuiteFitness contextFitness = this;
		Set<EPAAdjacentEdgesCoverageTestFitness> goalsCoveredByResult = EPAAdjacentEdgesMiningCoverageFactory.calculateEPAAdjacentEdgesMiningInfo(results, contextFitness);

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
		for (EPAAdjacentEdgesCoverageTestFitness knownCoverageGoal : EPAAdjacentEdgesMiningCoverageFactory.getGoals().values()) {
			if (!goalsCoveredByResult.contains(knownCoverageGoal)) {
				numUncoveredGoals++;
			}
		}

		if (numCoveredGoals > maxEPAAdjacentEdgesMiningGoalsCovered) {
			logger.info("(Adjacent Edges Pairs Mining) Best individual covers " + numCoveredGoals + " transitions");
			maxEPAAdjacentEdgesMiningGoalsCovered = numCoveredGoals;
		}

		// We cannot set a coverage here, as it does not make any sense
		// suite.setCoverage(this, 1.0);
//		double epaAdjacentEdgesMiningCoverageFitness = totalGoals - numCoveredGoals;
//		double epaAdjacentEdgesMiningCoverageFitness = 1d / (1d + numCoveredGoals);
		final double coverage = BigDecimal.valueOf(numCoveredGoals).divide(new BigDecimal(EPAAdjacentEdgesMiningCoverageFactory.UPPER_BOUND_OF_GOALS.toString()), 10, RoundingMode.HALF_EVEN).doubleValue();
		final double fitness = (1 - coverage);
		updateIndividual(this, suite, fitness);
		suite.setCoverage(this, coverage);
		suite.setNumOfCoveredGoals(this, numCoveredGoals);
		suite.setNumOfNotCoveredGoals(this, numUncoveredGoals);
		return fitness;
	}

}
