package org.evosuite.coverage.epa;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.evosuite.Properties;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.xml.sax.SAXException;

public class EPAAdjacentEdgesCoverageSuiteFitness extends TestSuiteFitnessFunction {

	private static final long serialVersionUID = -8137606898642577596L;
	private static int maxEPAAdjacentEdgesGoalsCovered = 0;

	public EPAAdjacentEdgesCoverageSuiteFitness(String epaXMLFilename) {

		try {
			EPA target_epa = EPAFactory.buildEPA(epaXMLFilename);
			EPASuiteFitness.checkEPAStates(target_epa, Properties.TARGET_CLASS);
			EPASuiteFitness.checkEPAActionNames(target_epa, Properties.TARGET_CLASS);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new EvosuiteError(e);
		}

	}

	@Override
	public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> suite) {
		final List<ExecutionResult> executionResults = runTestSuite(suite);
		EPAAdjacentEdgesCoverageSuiteFitness contextFitness = this;

		Set<EPAAdjacentEdgesCoverageTestFitness> goalsCoveredByResult = EPAAdjacentEdgesCoverageFactory.calculateEPAAdjacentEdgesInfo(executionResults, contextFitness);
		
		if (Properties.TEST_ARCHIVE) {
			// If we are using the archive, then fitness is by definition 0
			// as all assertions already covered are in the archive
			suite.setFitness(this, 0.0);
			suite.setCoverage(this, 1.0);
			maxEPAAdjacentEdgesGoalsCovered = EPAAdjacentEdgesCoverageFactory.getGoals().size();
			return 0.0;
		}
		
		int numCoveredGoals = goalsCoveredByResult.size();
		int numUncoveredGoals = 0;
		for (EPAAdjacentEdgesCoverageTestFitness knownCoverageGoal : EPAAdjacentEdgesCoverageFactory.getGoals().values()) {
			if (!goalsCoveredByResult.contains(knownCoverageGoal)) {
				numUncoveredGoals++;
			}
		}

		if (numCoveredGoals > maxEPAAdjacentEdgesGoalsCovered) {
			logger.info("(Adjacent Edges pairs) Best individual covers " + numCoveredGoals + " transitions");
			maxEPAAdjacentEdgesGoalsCovered = numCoveredGoals;
		}

//		double epaAdjacentEdgesCoverageFitness = EPAAdjacentEdgesCoverageFactory.UPPER_BOUND_OF_GOALS - numCoveredGoals;
		final double coverage = numCoveredGoals / (double) EPAAdjacentEdgesCoverageFactory.UPPER_BOUND_OF_GOALS;
		final double fitness = (1 - coverage);
		updateIndividual(this, suite, fitness);
		suite.setCoverage(this, coverage);
		suite.setNumOfCoveredGoals(this, numCoveredGoals);
		suite.setNumOfNotCoveredGoals(this, numUncoveredGoals);
		return fitness;
	}
	
}
