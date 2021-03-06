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

package org.drools.planner.config.heuristic.selector.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.collections.CollectionUtils;
import org.drools.planner.config.EnvironmentMode;
import org.drools.planner.config.heuristic.selector.SelectorConfig;
import org.drools.planner.config.heuristic.selector.common.SelectionOrder;
import org.drools.planner.config.util.ConfigUtils;
import org.drools.planner.core.domain.entity.PlanningEntityDescriptor;
import org.drools.planner.core.domain.solution.SolutionDescriptor;
import org.drools.planner.core.domain.variable.PlanningVariableDescriptor;
import org.drools.planner.core.heuristic.selector.common.SelectionCacheType;
import org.drools.planner.core.heuristic.selector.common.decorator.SelectionFilter;
import org.drools.planner.core.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import org.drools.planner.core.heuristic.selector.entity.EntitySelector;
import org.drools.planner.core.heuristic.selector.entity.FromSolutionEntitySelector;
import org.drools.planner.core.heuristic.selector.entity.decorator.CachingEntitySelector;
import org.drools.planner.core.heuristic.selector.entity.decorator.CachingFilteringEntitySelector;
import org.drools.planner.core.heuristic.selector.entity.decorator.JustInTimeFilteringEntitySelector;
import org.drools.planner.core.heuristic.selector.entity.decorator.ProbabilityEntitySelector;
import org.drools.planner.core.heuristic.selector.entity.decorator.ShufflingEntitySelector;

@XStreamAlias("entitySelector")
public class EntitySelectorConfig extends SelectorConfig {

    protected Class<?> planningEntityClass = null;

    protected SelectionOrder selectionOrder = null;
    protected SelectionCacheType cacheType = null;
    @XStreamImplicit(itemFieldName = "entityFilterClass")
    protected List<Class<? extends SelectionFilter>> entityFilterClassList = null;
    protected Class<? extends SelectionProbabilityWeightFactory> entityProbabilityWeightFactoryClass = null;
    // TODO sorterClass, decreasingDifficulty

    public Class<?> getPlanningEntityClass() {
        return planningEntityClass;
    }

    public void setPlanningEntityClass(Class<?> planningEntityClass) {
        this.planningEntityClass = planningEntityClass;
    }

    public SelectionOrder getSelectionOrder() {
        return selectionOrder;
    }

    public void setSelectionOrder(SelectionOrder selectionOrder) {
        this.selectionOrder = selectionOrder;
    }

    public SelectionCacheType getCacheType() {
        return cacheType;
    }

    public void setCacheType(SelectionCacheType cacheType) {
        this.cacheType = cacheType;
    }

    public List<Class<? extends SelectionFilter>> getEntityFilterClassList() {
        return entityFilterClassList;
    }

    public void setEntityFilterClassList(List<Class<? extends SelectionFilter>> entityFilterClassList) {
        this.entityFilterClassList = entityFilterClassList;
    }

    public Class<? extends SelectionProbabilityWeightFactory> getEntityProbabilityWeightFactoryClass() {
        return entityProbabilityWeightFactoryClass;
    }

    public void setEntityProbabilityWeightFactoryClass(Class<? extends SelectionProbabilityWeightFactory> entityProbabilityWeightFactoryClass) {
        this.entityProbabilityWeightFactoryClass = entityProbabilityWeightFactoryClass;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    /**
     * @param environmentMode never null
     * @param solutionDescriptor never null
     * @param inheritedSelectionOrder never null
     * @param minimumCacheType never null, If caching is used (different from {@link SelectionCacheType#JUST_IN_TIME}),
     * then it should be at least this {@link SelectionCacheType} because an ancestor already uses such caching
     * and less would be pointless.
     * @return never null
     */
    public EntitySelector buildEntitySelector(EnvironmentMode environmentMode, SolutionDescriptor solutionDescriptor,
            SelectionOrder inheritedSelectionOrder, SelectionCacheType minimumCacheType) {
        PlanningEntityDescriptor entityDescriptor = fetchEntityDescriptor(solutionDescriptor);
        SelectionOrder resolvedSelectionOrder = SelectionOrder.resolve(selectionOrder,
                inheritedSelectionOrder);
        SelectionCacheType resolvedCacheType = SelectionCacheType.resolve(cacheType, minimumCacheType);
        minimumCacheType = SelectionCacheType.max(minimumCacheType, resolvedCacheType);
        // FromSolutionEntitySelector caches by design, so it uses the minimumCacheType
        if (minimumCacheType.compareTo(SelectionCacheType.STEP) < 0) {
            // cacheType upgrades to SelectionCacheType.STEP (without shuffling) because JIT is not supported
            minimumCacheType = SelectionCacheType.STEP;
        }
        if (minimumCacheType == SelectionCacheType.SOLVER) {
            // TODO Solver cached entities are not compatible with DroolsScoreCalculator
            // because between phases the entities get cloned and the WorkingMemory contains those clones afterwards
            // https://issues.jboss.org/browse/JBRULES-3557
            throw new IllegalArgumentException("The minimumCacheType (" + minimumCacheType
                    + ") is not yet supported. Please use " + SelectionCacheType.PHASE + " instead.");
        }
        EntitySelector entitySelector = new FromSolutionEntitySelector(entityDescriptor,
                (resolvedCacheType.isCached() ? SelectionOrder.ORIGINAL : resolvedSelectionOrder)
                        == SelectionOrder.RANDOM,
                minimumCacheType);

        boolean alreadyCached = false;
        if (!CollectionUtils.isEmpty(entityFilterClassList)
                || entityDescriptor.hasMovableEntitySelectionFilter()) {
            List<SelectionFilter> entityFilterList = new ArrayList<SelectionFilter>(
                    entityFilterClassList == null ? 1 : entityFilterClassList.size());
            if (entityFilterClassList != null) {
                for (Class<? extends SelectionFilter> entityFilterClass : entityFilterClassList) {
                    entityFilterList.add(ConfigUtils.newInstance(this, "entityFilterClass", entityFilterClass));
                }
            }
            if (entityDescriptor.hasMovableEntitySelectionFilter()) {
                entityFilterList.add(entityDescriptor.getMovableEntitySelectionFilter());
            }
            EntitySelector filteringEntitySelector;
            if (resolvedCacheType == SelectionCacheType.JUST_IN_TIME) {
                filteringEntitySelector = new JustInTimeFilteringEntitySelector(entitySelector,
                        resolvedCacheType, entityFilterList);
            } else {
                filteringEntitySelector = new CachingFilteringEntitySelector(entitySelector,
                        resolvedCacheType, entityFilterList);
                alreadyCached = true;
            }
            entitySelector = filteringEntitySelector;
        }
        // TODO entitySorterClass
        if (entityProbabilityWeightFactoryClass != null) {
            if (resolvedSelectionOrder != SelectionOrder.RANDOM) {
                throw new IllegalArgumentException("The entitySelectorConfig (" + this
                        + ") with entityProbabilityWeightFactoryClass ("
                        + entityProbabilityWeightFactoryClass + ") has a non-random resolvedSelectionOrder ("
                        + resolvedSelectionOrder + ").");
            }
            SelectionProbabilityWeightFactory entityProbabilityWeightFactory = ConfigUtils.newInstance(this,
                    "entityProbabilityWeightFactoryClass", entityProbabilityWeightFactoryClass);
            entitySelector = new ProbabilityEntitySelector(entitySelector,
                    resolvedCacheType, entityProbabilityWeightFactory);
            alreadyCached = true;
        }
        if (resolvedCacheType.isCached() && !alreadyCached) {
            if (resolvedSelectionOrder != SelectionOrder.RANDOM) {
                // TODO this is pretty pointless, because FromSolutionEntitySelector caches
                entitySelector = new CachingEntitySelector(entitySelector, resolvedCacheType);
            } else {
                // Not pointless, because FromSolutionEntitySelector does not shuffle
                entitySelector = new ShufflingEntitySelector(entitySelector, resolvedCacheType);
            }
        }
        return entitySelector;
    }

    private PlanningEntityDescriptor fetchEntityDescriptor(SolutionDescriptor solutionDescriptor) {
        PlanningEntityDescriptor entityDescriptor;
        if (planningEntityClass != null) {
            entityDescriptor = solutionDescriptor.getPlanningEntityDescriptorStrict(planningEntityClass);
            if (entityDescriptor == null) {
                throw new IllegalArgumentException("The entitySelectorConfig (" + this + ") has a planningEntityClass ("
                        + planningEntityClass + ") that is not configured as a planningEntity.\n" +
                        "If that class (" + planningEntityClass.getSimpleName() + ") is not a " +
                        "planningEntityClass (" + solutionDescriptor.getPlanningEntityClassSet()
                        + "), check your Solution implementation's annotated methods.\n" +
                        "If it is, check your solver configuration.");
            }
        } else {
            Collection<PlanningEntityDescriptor> planningEntityDescriptors = solutionDescriptor
                    .getPlanningEntityDescriptors();
            if (planningEntityDescriptors.size() != 1) {
                throw new IllegalArgumentException("The entitySelectorConfig (" + this
                        + ") has no configured planningEntityClass ("
                        + planningEntityClass + ") and because there are multiple in the planningEntityClassSet ("
                        + solutionDescriptor.getPlanningEntityClassSet()
                        + "), it can not be deducted automatically.");
            }
            entityDescriptor = planningEntityDescriptors.iterator().next();
        }
        return entityDescriptor;
    }

    public void inherit(EntitySelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        planningEntityClass = ConfigUtils.inheritOverwritableProperty(planningEntityClass,
                inheritedConfig.getPlanningEntityClass());
        selectionOrder = ConfigUtils.inheritOverwritableProperty(selectionOrder, inheritedConfig.getSelectionOrder());
        cacheType = ConfigUtils.inheritOverwritableProperty(cacheType, inheritedConfig.getCacheType());
        entityFilterClassList = ConfigUtils.inheritOverwritableProperty
                (entityFilterClassList, inheritedConfig.getEntityFilterClassList());
        entityProbabilityWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(
                entityProbabilityWeightFactoryClass, inheritedConfig.getEntityProbabilityWeightFactoryClass());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + planningEntityClass + ")";
    }

}
