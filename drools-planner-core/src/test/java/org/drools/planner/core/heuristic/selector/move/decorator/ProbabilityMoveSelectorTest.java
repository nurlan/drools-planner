/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.planner.core.heuristic.selector.move.decorator;

import java.util.Iterator;
import java.util.Random;

import org.drools.planner.core.heuristic.selector.SelectorTestUtils;
import org.drools.planner.core.heuristic.selector.common.SelectionCacheType;
import org.drools.planner.core.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import org.drools.planner.core.heuristic.selector.move.MoveSelector;
import org.drools.planner.core.heuristic.selector.move.decorator.ProbabilityMoveSelector;
import org.drools.planner.core.move.DummyMove;
import org.drools.planner.core.move.Move;
import org.drools.planner.core.phase.AbstractSolverPhaseScope;
import org.drools.planner.core.phase.step.AbstractStepScope;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.core.solver.DefaultSolverScope;
import org.junit.Test;
import org.mockito.Matchers;

import static org.drools.planner.core.testdata.util.PlannerAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ProbabilityMoveSelectorTest {

    @Test
    public void randomSelection() {
        MoveSelector childMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class,
                new DummyMove("e1"), new DummyMove("e2"), new DummyMove("e3"), new DummyMove("e4"));

        SelectionProbabilityWeightFactory<DummyMove> probabilityWeightFactory = new SelectionProbabilityWeightFactory<DummyMove>() {
            public double createProbabilityWeight(ScoreDirector scoreDirector, DummyMove move) {
                if (move.getCode().equals("e1")) {
                    return 1000.0;
                } else if (move.getCode().equals("e2")) {
                    return 200.0;
                } else if (move.getCode().equals("e3")) {
                    return 30.0;
                } else if (move.getCode().equals("e4")) {
                    return 4.0;
                } else {
                    throw new IllegalStateException("Unknown move (" + move + ").");
                }
            }
        };
        MoveSelector moveSelector = new ProbabilityMoveSelector(childMoveSelector, SelectionCacheType.STEP,
                probabilityWeightFactory);

        Random workingRandom = mock(Random.class);
        when(workingRandom.nextDouble()).thenReturn(1222.0 / 1234.0, 111.0 / 1234.0, 0.0, 1230.0 / 1234.0, 1199.0 / 1234.0);

        DefaultSolverScope solverScope = mock(DefaultSolverScope.class);
        when(solverScope.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.solvingStarted(solverScope);
        AbstractSolverPhaseScope phaseScopeA = mock(AbstractSolverPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        when(phaseScopeA.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getSolverPhaseScope()).thenReturn(phaseScopeA);
        when(stepScopeA1.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.stepStarted(stepScopeA1);

        assertEquals(false, moveSelector.isContinuous());
        assertEquals(true, moveSelector.isNeverEnding());
        assertEquals(4L, moveSelector.getSize());
        Iterator<Move> iterator = moveSelector.iterator();
        assertTrue(iterator.hasNext());
        assertCode("e3", iterator.next());
        assertTrue(iterator.hasNext());
        assertCode("e1", iterator.next());
        assertTrue(iterator.hasNext());
        assertCode("e1", iterator.next());
        assertTrue(iterator.hasNext());
        assertCode("e4", iterator.next());
        assertTrue(iterator.hasNext());
        assertCode("e2", iterator.next());
        assertTrue(iterator.hasNext());

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verify(childMoveSelector, times(1)).solvingStarted(solverScope);
        verify(childMoveSelector, times(1)).phaseStarted(Matchers.<AbstractSolverPhaseScope>any());
        verify(childMoveSelector, times(1)).stepStarted(Matchers.<AbstractStepScope>any());
        verify(childMoveSelector, times(1)).stepEnded(Matchers.<AbstractStepScope>any());
        verify(childMoveSelector, times(1)).phaseEnded(Matchers.<AbstractSolverPhaseScope>any());
        verify(childMoveSelector, times(1)).solvingEnded(solverScope);
        verify(childMoveSelector, times(1)).iterator();
    }

}
