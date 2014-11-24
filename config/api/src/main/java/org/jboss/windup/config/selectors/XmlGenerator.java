package org.jboss.windup.config.selectors;

import org.jboss.windup.config.WindupRuleProvider;
import org.jboss.windup.graph.GraphContext;

public interface XmlGenerator
{
    public void process(WindupRuleProvider provider, GraphContext context);
}
