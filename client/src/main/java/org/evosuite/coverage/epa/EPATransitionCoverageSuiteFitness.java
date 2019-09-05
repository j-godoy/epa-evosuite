package org.evosuite.coverage.epa;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.evosuite.Properties;
import org.evosuite.coverage.archive.TestsArchive;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.xml.sax.SAXException;

/**
 * This fitness function counts the degree of covered normal transitions in the
 * EPA automata. It is a minimization function (less is better). The value 0.0
 * means all transitions were covered.
 * 
 * @author galeotti
 *
 */
public class EPATransitionCoverageSuiteFitness extends TestSuiteFitnessFunction {

	private static final long serialVersionUID = -130694090671398370L;
	private final List<EPATransitionCoverageTestFitness> goals;
	private final EPA epa;

	public EPATransitionCoverageSuiteFitness(String epaXMLFilename) {
		EPA target_epa;
		try {
			target_epa = EPAFactory.buildEPA(epaXMLFilename);
			EPASuiteFitness.checkEPAStates(target_epa, Properties.TARGET_CLASS);
			EPASuiteFitness.checkEPAActionNames(target_epa, Properties.TARGET_CLASS);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new EvosuiteError(e);
		}
		EPATransitionCoverageFactory goalFactory = new EPATransitionCoverageFactory(target_epa);
		this.goals = goalFactory.getCoverageGoals();
		this.epa = target_epa;
 	}

	@Override
	public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> suite) {
		final List<ExecutionResult> executionResults = runTestSuite(suite);
		
		Set<EPATransitionCoverageTestFitness> goalsCoveredByResult = calculateEPATransitionInfo(executionResults, this);
		
		if (Properties.TEST_ARCHIVE) {
			// If we are using the archive, then fitness is by definition 0
			// as all assertions already covered are in the archive
			suite.setFitness(this, 0.0);
			suite.setCoverage(this, 1.0);
			return 0.0;
		}
		
		long coveredGoalsCount = goalsCoveredByResult.size();
		final double coverage = (this.goals.size() > 0) ? (coveredGoalsCount / (double) this.goals.size()) : 1;
		final double fitness = (1 - coverage);
		updateIndividual(this, suite, fitness);
		suite.setCoverage(this, coverage);
		suite.setNumOfCoveredGoals(this, (int) coveredGoalsCount);
		suite.setNumOfNotCoveredGoals(this, (int) (this.goals.size() - coveredGoalsCount));
		return fitness;
	}
	
	
	private Set<EPATransitionCoverageTestFitness> calculateEPATransitionInfo(List<ExecutionResult> results, EPATransitionCoverageSuiteFitness contextFitness) {
		Set<EPATransitionCoverageTestFitness> goalsCoveredByResults = new HashSet<>();
		
		for (ExecutionResult result : results) {
			for (EPATrace epa_trace : result.getTrace().getEPATraces()) {
				for (EPATransition epa_transition : epa_trace.getEpaTransitions()) {
					if (epa_transition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)) {
						// discard the rest of the trace if an invalid object state is reached
						break;
					}
					
					EPATransitionCoverageGoal g = new EPATransitionCoverageGoal(Properties.TARGET_CLASS, epa, epa_transition);
					EPATransitionCoverageTestFitness goal = new EPATransitionCoverageTestFitness(g);
					
					if (this.goals.contains(goal)) {
						goalsCoveredByResults.add(goal);
					}

					if (Properties.TEST_ARCHIVE && goal != null) {
						TestsArchive.instance.addGoalToCover(contextFitness, goal);
						TestsArchive.instance.putTest(contextFitness, goal, result);
					}
				}
			}
		}
		
		return goalsCoveredByResults;
	}
	

}