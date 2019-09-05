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

public class EPAExceptionCoverageSuiteFitness extends TestSuiteFitnessFunction {

	private static final long serialVersionUID = -3588790421141471290L;
	private static int maxEPAExceptionGoalsCovered = 0;

	public EPAExceptionCoverageSuiteFitness(String epaXMLFilename) {

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
		logger.trace("Calculating EPAException fitness");

		List<ExecutionResult> results = runTestSuite(suite);
		EPAExceptionCoverageSuiteFitness contextFitness = this;
		Set<EPAExceptionCoverageTestFitness> goalsCoveredByResult = EPAExceptionCoverageFactory.calculateEPAExceptionInfo(results, contextFitness);

		if (Properties.TEST_ARCHIVE) {
			// If we are using the archive, then fitness is by definition 0
			// as all assertions already covered are in the archive
			suite.setFitness(this, 0.0);
			suite.setCoverage(this, 1.0);
			maxEPAExceptionGoalsCovered = EPAExceptionCoverageFactory.getGoals().size();
			return 0.0;
		}

		int numCoveredGoals = goalsCoveredByResult.size();
		int numUncoveredGoals = 0;
		for (EPAExceptionCoverageTestFitness knownCoverageGoal : EPAExceptionCoverageFactory.getGoals().values()) {
			if (!goalsCoveredByResult.contains(knownCoverageGoal)) {
				numUncoveredGoals++;
			}
		}

		if (numCoveredGoals > maxEPAExceptionGoalsCovered) {
			logger.info("(Exceptions) Best individual covers " + numCoveredGoals + " transitions");
			maxEPAExceptionGoalsCovered = numCoveredGoals;
		}

		// We cannot set a coverage here, as it does not make any sense
		// suiteChromosome.setCoverage(this, 1.0);
		final double coverage = (double) numCoveredGoals / EPAExceptionCoverageFactory.UPPER_BOUND_OF_GOALS;
		final double fitness = (1 - coverage);
		updateIndividual(this, suite, fitness);
		suite.setCoverage(this, coverage);
		suite.setNumOfCoveredGoals(this, numCoveredGoals);
		suite.setNumOfNotCoveredGoals(this, numUncoveredGoals);
		return fitness;
	}
	
}
