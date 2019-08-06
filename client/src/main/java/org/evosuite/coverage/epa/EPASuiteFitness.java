package org.evosuite.coverage.epa;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.archive.TestsArchive;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.xml.sax.SAXException;

public abstract class EPASuiteFitness extends TestSuiteFitnessFunction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9069407017356785315L;

	private final EPA epa;

	private static Map<String, EPATransitionCoverageTestFitness> coverageGoalMap = null;
	private static Map<String, EPATransitionCoverageTestFitness> nonCoverageGoalMap = null;
	//public final Set<String> toRemoveTransitions = new HashSet<String>();

	public EPASuiteFitness(String epaXMLFilename) {
		if (epaXMLFilename == null) {
			throw new IllegalArgumentException("epa XML Filename cannot be null");
		}
		try {
			EPA target_epa = EPAFactory.buildEPA(epaXMLFilename);
			checkEPAStates(target_epa, Properties.TARGET_CLASS);
			checkEPAActionNames(target_epa, Properties.TARGET_CLASS);

			this.epa = target_epa;
			if(coverageGoalMap == null) {
				coverageGoalMap = this.buildCoverageGoalMap(getGoalFactory(this.epa));
				nonCoverageGoalMap = new HashMap<>(coverageGoalMap);
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new EvosuiteError(e);
		}
	}

	public Map<String, EPATransitionCoverageTestFitness> getCoverageGoalMap() {
		return coverageGoalMap;
	}

	public Map<String, EPATransitionCoverageTestFitness> getNonCoverageGoalMap() {
		return nonCoverageGoalMap;
	}

	public EPA getEPA() {
		return epa;
	}

	protected abstract EPAFitnessFactory getGoalFactory(EPA epa);

	private Map<String, EPATransitionCoverageTestFitness> buildCoverageGoalMap(EPAFitnessFactory goalFactory) {
		Map<String, EPATransitionCoverageTestFitness> coverageGoalMap = new HashMap<>();
		for (EPATransitionCoverageTestFitness goal : goalFactory.getCoverageGoals()) {
			coverageGoalMap.put(goal.getGoalName(), goal);
			if(Properties.TEST_ARCHIVE)
				TestsArchive.instance.addGoalToCover(this, goal);
		}
		return coverageGoalMap;
	}

	protected static void checkEPAStates(EPA epa, String className) {
		try {
			for (EPAState epaState : epa.getStates()) {
				if (epa.getInitialState().equals(epaState)) {
					continue; // initial states are false by default
				}
				Class<?> clazz = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(className);
				if (!EPAUtils.epaStateMethodExists(epaState, clazz)) {
					throw new EvosuiteError("Boolean query method for EPA State " + epaState.getName()
							+ " was not found in target class " + className);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new EvosuiteError(e);
		}
	}

	protected static void checkEPAActionNames(EPA epa, String className) {
		try {
			Class<?> clazz = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(className);
			for (String actionName : epa.getActions()) {
				final boolean found = !EPAUtils.getEpaActionMethods(actionName, clazz).isEmpty()
						|| !EPAUtils.getEpaActionConstructors(actionName, clazz).isEmpty();
				if (!found) {
					throw new EvosuiteError(
							"EPA Action Name " + actionName + "was not found in target class " + className);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new EvosuiteError(e);
		}
	}

	@Override
	public final double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> suiteChromosome) {

		long coveredGoalsCount = 0;
		final List<ExecutionResult> executionResults = runTestSuite(suiteChromosome);
		final Collection<EPATransitionCoverageTestFitness> goals = getCoverageGoalMap().values();
		for (EPATransitionCoverageTestFitness goal : goals) {
			for(ExecutionResult result : executionResults)
			{
				if (goal.isCovered(result)) {
					nonCoverageGoalMap.remove(goal.getGoalName(), goal);
					coveredGoalsCount++;
					if (Properties.TEST_ARCHIVE && this != null) {
						//toRemoveTransitions.add(goal.getGoalName());
						TestsArchive.instance.addGoalToCover(this, goal);
						TestsArchive.instance.putTest(this, goal, result);
						suiteChromosome.isToBeUpdated(true);
					}
					break;
				}
			}
		}
		final double fitness = goals.size() - coveredGoalsCount;
		updateIndividual(this, suiteChromosome, fitness);
		final double coverage = (goals.size() > 0) ? (coveredGoalsCount / (double) goals.size()) : 0;
		suiteChromosome.setCoverage(this, coverage);
		suiteChromosome.setNumOfCoveredGoals(this, (int) coveredGoalsCount);
		suiteChromosome.setNumOfNotCoveredGoals(this, (int) (goals.size() - coveredGoalsCount));
		return fitness;
	}

}
