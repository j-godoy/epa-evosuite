package org.evosuite.coverage.epa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.evosuite.Properties;
import org.evosuite.testsuite.AbstractFitnessFactory;

public class EPATransitionCoverageFactory extends AbstractFitnessFactory<EPATransitionCoverageTestFitness> {
	
	private static Map<String, EPATransitionCoverageTestFitness> goals;

	public EPATransitionCoverageFactory(EPA epaAutomata) {
		EPASuiteFitness.checkEPAStates(epaAutomata, Properties.TARGET_CLASS);
		EPASuiteFitness.checkEPAActionNames(epaAutomata, Properties.TARGET_CLASS);
		EPATransitionCoverageFactory.goals = buildCoverageGoalMap(epaAutomata);
	}

	@Override
	public List<EPATransitionCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPATransitionCoverageTestFitness>(goals.values());
	}
	
	public static Map<String, EPATransitionCoverageTestFitness> getGoals() {
        return goals;
    }
	
	private Map<String, EPATransitionCoverageTestFitness> buildCoverageGoalMap(EPA epa) {
		Map<String, EPATransitionCoverageTestFitness> coverageGoalMap = new HashMap<>();
		for(EPATransition t : epa.getTransitions())
		{
			EPATransitionCoverageGoal g = new EPATransitionCoverageGoal(Properties.TARGET_CLASS, epa, t);
			EPATransitionCoverageTestFitness goal = new EPATransitionCoverageTestFitness(g);
			coverageGoalMap.put(goal.getGoalName(), goal);
		}
		return coverageGoalMap;
	}

	
}