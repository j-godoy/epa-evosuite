package org.evosuite.coverage.epa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.evosuite.testsuite.AbstractFitnessFactory;

public class EPAAdjacentEdgesMiningCoverageFactory extends AbstractFitnessFactory<EPAAdjacentEdgesMiningCoverageTestFitness> {

    private static Map<String, EPAAdjacentEdgesMiningCoverageTestFitness> goals = new LinkedHashMap<>();

    public static Map<String, EPAAdjacentEdgesMiningCoverageTestFitness> getGoals() {
        return goals;
    }

    /** {@inheritDoc} */
	@Override
	public List<EPAAdjacentEdgesMiningCoverageTestFitness> getCoverageGoals() {
		return new ArrayList<EPAAdjacentEdgesMiningCoverageTestFitness>(goals.values());
	}
}
