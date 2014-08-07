/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.windup.config.typeiteration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.RulePhase;
import org.jboss.windup.config.WindupRuleProvider;
import org.jboss.windup.config.condition.GraphCondition;
import org.jboss.windup.config.operation.GraphOperation;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.config.operation.ruleelement.AbstractIterationFilter;
import org.jboss.windup.config.operation.ruleelement.AbstractIterationOperation;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.graph.GraphContext;
import org.junit.Assert;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;

/**
 * Test Rule Provider for the purpose of testing the iteration.over(type).
 * @author mbriskar
 *
 */
public class IterationOverTypesRuleProvider extends WindupRuleProvider
{
    public static int performCounter = 0;
    public static int otherwiseCounter = 0;
    @Override
    public RulePhase getPhase()
    {
        return RulePhase.MIGRATION_RULES;
    }

    // @formatter:off
    @Override
    public Configuration getConfiguration(GraphContext context)
    {
        Configuration configuration = ConfigurationBuilder.begin()
        .addRule()
        .when(Query.find(TestSimple1Model.class))
        .perform(Iteration
            .over(TestSimple1Model.class)
            .perform(new GraphOperation()
            {
                @Override
                public void perform(GraphRewrite event, EvaluationContext context)
                {
                    performCounter++;
                }
            })
            .endIteration()
        ).otherwise(new GraphOperation() {
            
            @Override
            public void perform(GraphRewrite event, EvaluationContext context)
            {
                otherwiseCounter++;
            }
        });
        return configuration;
    }

}
