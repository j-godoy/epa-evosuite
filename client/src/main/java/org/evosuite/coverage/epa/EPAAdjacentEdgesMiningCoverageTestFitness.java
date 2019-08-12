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

import org.evosuite.testcase.*;
import org.evosuite.testcase.execution.ExecutionResult;

public class EPAAdjacentEdgesMiningCoverageTestFitness extends TestFitnessFunction {

	private static final long serialVersionUID = 7659713027558487726L;
	
	private final EPAAdjacentEdgesPair epaAdjacentPairs;

	private final String targetClass;

	public EPAAdjacentEdgesMiningCoverageTestFitness(String targetClass, EPATransition firstTransition, EPATransition secondTransition) {
		this.targetClass = targetClass;
		this.epaAdjacentPairs = new EPAAdjacentEdgesPair(firstTransition, secondTransition);
	}

	public String getKey() {
		return this.epaAdjacentPairs.toString();
	}

	/**
	 * {@inheritDoc}
	 *
	 * Calculate fitness
	 *
	 * @param individual
	 *            a {@link org.evosuite.testcase.ExecutableChromosome} object.
	 * @param result
	 *            a {@link org.evosuite.testcase.execution.ExecutionResult} object.
	 * @return a double.
	 */
	@Override
	public double getFitness(TestChromosome individual, ExecutionResult result) {
		double fitness = 1.0;

		for (EPATrace epa_trace : result.getTrace().getEPATraces()) {
			for (int i = 0; i < epa_trace.getEpaTransitions().size()-1; i++) {
				EPATransition firstEpaTransition = epa_trace.getEpaTransitions().get(i);
				EPATransition secondEpaTransition = epa_trace.getEpaTransitions().get(i + 1);
				if (firstEpaTransition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)
						|| secondEpaTransition.getDestinationState().equals(EPAState.INVALID_OBJECT_STATE)) {
					// discard the rest of the trace if an invalid object state is reached
					break;
				}
				
				if (isCoveredBy(firstEpaTransition, secondEpaTransition)) {
					return 0.0;
				}
			}
		}

		updateIndividual(this, individual, fitness);

		return fitness;
	}
	
	public boolean isCoveredBy(EPATransition firstEpaTransition, EPATransition secondEpaTransition)
	{
		EPAAdjacentEdgesPair coveredAdjacentEdges = new EPAAdjacentEdgesPair(firstEpaTransition, secondEpaTransition);
		return this.epaAdjacentPairs.equals(coveredAdjacentEdges);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getKey();
	}

	@Override
	public int compareTo(TestFitnessFunction other) {
		if (other instanceof EPAAdjacentEdgesCoverageTestFitness) {
			EPAAdjacentEdgesCoverageTestFitness otherEPAAdjacentEdgesFitness = (EPAAdjacentEdgesCoverageTestFitness) other;
			return this.epaAdjacentPairs.toString().compareTo(otherEPAAdjacentEdgesFitness.toString());
		}
		return compareClassName(other);
	}

	@Override
	public String getTargetClass() {
		return targetClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((epaAdjacentPairs == null) ? 0 : epaAdjacentPairs.hashCode());
		result = prime * result + ((targetClass == null) ? 0 : targetClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EPAAdjacentEdgesMiningCoverageTestFitness other = (EPAAdjacentEdgesMiningCoverageTestFitness) obj;
		if (epaAdjacentPairs == null) {
			if (other.epaAdjacentPairs != null)
				return false;
		} else if (!epaAdjacentPairs.equals(other.epaAdjacentPairs))
			return false;
		if (targetClass == null) {
			if (other.targetClass != null)
				return false;
		} else if (!targetClass.equals(other.targetClass))
			return false;
		return true;
	}

	@Override
	public String getTargetMethod() {
		return this.epaAdjacentPairs.getMethodName();
	}

	

}