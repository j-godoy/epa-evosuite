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
	public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> suiteChromosome) {
		logger.trace("Calculating EPAException fitness");


		int maxNumberOfAutomataTransitions = EPAExceptionCoverageFactory.UPPER_BOUND_OF_GOALS;

		List<ExecutionResult> results = runTestSuite(suiteChromosome);
		EPAExceptionCoverageSuiteFitness contextFitness = this;
		Set<EPAExceptionCoverageTestFitness> goalsCoveredByResult = EPAExceptionCoverageFactory.calculateEPAExceptionInfo(results, contextFitness);

		if (Properties.TEST_ARCHIVE) {
			// If we are using the archive, then fitness is by definition 0
			// as all assertions already covered are in the archive
			suiteChromosome.setFitness(this, 0.0);
			suiteChromosome.setCoverage(this, 1.0);
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
		double epaExceptionCoverageFitness = maxNumberOfAutomataTransitions - numCoveredGoals;

		suiteChromosome.setFitness(this, epaExceptionCoverageFitness);
		if (maxEPAExceptionGoalsCovered > 0)
			suiteChromosome.setCoverage(this, numCoveredGoals / maxEPAExceptionGoalsCovered);
		else
			suiteChromosome.setCoverage(this, 1.0);

		suiteChromosome.setNumOfCoveredGoals(this, numCoveredGoals);
		suiteChromosome.setNumOfNotCoveredGoals(this, numUncoveredGoals);
		return epaExceptionCoverageFitness;
	}
	
}
