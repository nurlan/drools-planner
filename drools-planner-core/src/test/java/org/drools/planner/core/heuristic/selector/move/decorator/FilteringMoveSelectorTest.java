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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.drools.planner.core.heuristic.selector.SelectorTestUtils;
import org.drools.planner.core.heuristic.selector.common.SelectionCacheType;
import org.drools.planner.core.heuristic.selector.common.decorator.SelectionFilter;
import org.drools.planner.core.heuristic.selector.move.MoveSelector;
import org.drools.planner.core.move.DummyMove;
import org.drools.planner.core.move.Move;
import org.drools.planner.core.phase.AbstractSolverPhaseScope;
import org.drools.planner.core.phase.step.AbstractStepScope;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.core.solver.DefaultSolverScope;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.drools.planner.core.testdata.util.PlannerAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FilteringMoveSelectorTest {

    @Test
    public void cacheTypeSolver() {
        runCacheType(SelectionCacheType.SOLVER, 1);
    }

    @Test
    public void cacheTypePhase() {
        runCacheType(SelectionCacheType.PHASE, 2);
    }

    @Test
    public void cacheTypeStep() {
        runCacheType(SelectionCacheType.STEP, 5);
    }

    @Test
    public void cacheTypeJustInTime() {
        runCacheType(SelectionCacheType.JUST_IN_TIME, 5);
    }

    public void runCacheType(SelectionCacheType cacheType, int timesCalled) {
        MoveSelector childMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class,
                new DummyMove("e1"), new DummyMove("e2"), new DummyMove("e3"), new DummyMove("e4"));

        SelectionFilter<DummyMove> moveFilter = new SelectionFilter<DummyMove>() {
            public boolean accept(ScoreDirector scoreDirector, DummyMove move) {
                return !move.getCode().equals("e3");
            }
        };
        List<SelectionFilter> moveFilterList = Arrays.<SelectionFilter>asList(moveFilter);
        MoveSelector moveSelector = cacheType == SelectionCacheType.JUST_IN_TIME
                ? new JustInTimeFilteringMoveSelector(childMoveSelector, cacheType, moveFilterList)
                : new CachingFilteringMoveSelector(childMoveSelector, cacheType, moveFilterList);

        DefaultSolverScope solverScope = mock(DefaultSolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractSolverPhaseScope phaseScopeA = mock(AbstractSolverPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getSolverPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        runAsserts(moveSelector, cacheType);
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getSolverPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        runAsserts(moveSelector, cacheType);
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractSolverPhaseScope phaseScopeB = mock(AbstractSolverPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getSolverPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        runAsserts(moveSelector, cacheType);
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getSolverPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        runAsserts(moveSelector, cacheType);
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getSolverPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        runAsserts(moveSelector, cacheType);
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verify(childMoveSelector, times(1)).solvingStarted(solverScope);
        verify(childMoveSelector, times(2)).phaseStarted(Matchers.<AbstractSolverPhaseScope>any());
        verify(childMoveSelector, times(5)).stepStarted(Matchers.<AbstractStepScope>any());
        verify(childMoveSelector, times(5)).stepEnded(Matchers.<AbstractStepScope>any());
        verify(childMoveSelector, times(2)).phaseEnded(Matchers.<AbstractSolverPhaseScope>any());
        verify(childMoveSelector, times(1)).solvingEnded(solverScope);
        verify(childMoveSelector, times(timesCalled)).iterator();
        verify(childMoveSelector, times(timesCalled)).getSize();
    }

    private void runAsserts(MoveSelector moveSelector, SelectionCacheType cacheType) {
        Iterator<Move> iterator = moveSelector.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertCode("e1", iterator.next());
        assertTrue(iterator.hasNext());
        assertCode("e2", iterator.next());
        assertTrue(iterator.hasNext());
        assertCode("e4", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(false, moveSelector.isContinuous());
        assertEquals(false, moveSelector.isNeverEnding());
        assertEquals((cacheType == SelectionCacheType.JUST_IN_TIME ? 4L : 3L), moveSelector.getSize());
    }

}
