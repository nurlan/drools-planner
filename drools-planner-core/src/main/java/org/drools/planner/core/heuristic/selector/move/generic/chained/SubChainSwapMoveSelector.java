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

package org.drools.planner.core.heuristic.selector.move.generic.chained;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.drools.planner.core.domain.variable.PlanningVariableDescriptor;
import org.drools.planner.core.heuristic.selector.common.iterator.AbstractOriginalSwapIterator;
import org.drools.planner.core.heuristic.selector.common.iterator.AbstractRandomSwapIterator;
import org.drools.planner.core.heuristic.selector.move.generic.GenericMoveSelector;
import org.drools.planner.core.heuristic.selector.value.chained.SubChain;
import org.drools.planner.core.heuristic.selector.value.chained.SubChainSelector;
import org.drools.planner.core.move.Move;

public class SubChainSwapMoveSelector extends GenericMoveSelector {

    protected final SubChainSelector leftSubChainSelector;
    protected final SubChainSelector rightSubChainSelector;
    protected final PlanningVariableDescriptor variableDescriptor;
    protected final boolean randomSelection;
    protected final boolean selectReversingMoveToo;

    public SubChainSwapMoveSelector(SubChainSelector leftSubChainSelector, SubChainSelector rightSubChainSelector,
            boolean randomSelection, boolean selectReversingMoveToo) {
        this.leftSubChainSelector = leftSubChainSelector;
        this.rightSubChainSelector = rightSubChainSelector;
        this.randomSelection = randomSelection;
        this.selectReversingMoveToo = selectReversingMoveToo;
        variableDescriptor = leftSubChainSelector.getVariableDescriptor();
        if (leftSubChainSelector.getVariableDescriptor() != rightSubChainSelector.getVariableDescriptor()) {
            throw new IllegalStateException("The moveSelector (" + this.getClass()
                    + ") has a leftSubChainSelector's variableDescriptor ("
                    + leftSubChainSelector.getVariableDescriptor()
                    + ") which is not equal to the rightSubChainSelector's variableDescriptor ("
                    + rightSubChainSelector.getVariableDescriptor() + ").");
        }
        solverPhaseLifecycleSupport.addEventListener(leftSubChainSelector);
        if (leftSubChainSelector != rightSubChainSelector) {
            solverPhaseLifecycleSupport.addEventListener(rightSubChainSelector);
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public boolean isContinuous() {
        return leftSubChainSelector.isContinuous() || rightSubChainSelector.isContinuous();
    }

    public boolean isNeverEnding() {
        return randomSelection || leftSubChainSelector.isNeverEnding() || rightSubChainSelector.isNeverEnding();
    }

    public long getSize() {
        return AbstractOriginalSwapIterator.getSize(leftSubChainSelector, rightSubChainSelector);
    }

    public Iterator<Move> iterator() {
        if (!randomSelection) {
            return new AbstractOriginalSwapIterator<Move, SubChain>(leftSubChainSelector, rightSubChainSelector) {
                private Move nextReversingSelection = null;

                @Override
                protected void createUpcomingSelection() {
                    if (selectReversingMoveToo && nextReversingSelection != null) {
                        upcomingSelection = nextReversingSelection;
                        nextReversingSelection = null;
                        return;
                    }
                    super.createUpcomingSelection();
                }

                @Override
                protected Move newSwapSelection(SubChain leftSubSelection, SubChain rightSubSelection) {
                    if (selectReversingMoveToo) {
                        nextReversingSelection
                                = new SubChainReversingSwapMove(variableDescriptor, leftSubSelection, rightSubSelection);
                    }
                    return new SubChainSwapMove(variableDescriptor, leftSubSelection, rightSubSelection);
                }
            };
        } else {
            return new AbstractRandomSwapIterator<Move, SubChain>(leftSubChainSelector, rightSubChainSelector) {
                @Override
                protected Move newSwapSelection(SubChain leftSubSelection, SubChain rightSubSelection) {
                    boolean reversing = selectReversingMoveToo ? workingRandom.nextBoolean() : false;
                    return reversing
                            ? new SubChainReversingSwapMove(variableDescriptor, leftSubSelection, rightSubSelection)
                            : new SubChainSwapMove(variableDescriptor, leftSubSelection, rightSubSelection);
                }
            };
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + leftSubChainSelector + ", " + rightSubChainSelector + ")";
    }

}
