package org.evosuite.coverage.epa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.evosuite.testsuite.AbstractFitnessFactory;

public class EPAExceptionMiningCoverageFactory extends AbstractFitnessFactory<EPAExceptionMiningCoverageTestFitness> {

    private static Map<String, EPAExceptionMiningCoverageTestFitness> goals = new LinkedHashMap<>();

    public static Map<String, EPAExceptionMiningCoverageTestFitness> getGoals() {
        return goals;
    }

    /** {@inheritDoc} */
	@Override
	public List<EPAExceptionMiningCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAExceptionMiningCoverageTestFitness>(goals.values());
	}
}
