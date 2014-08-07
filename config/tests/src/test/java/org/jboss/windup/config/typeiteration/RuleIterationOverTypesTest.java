package org.jboss.windup.config.typeiteration;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.windup.config.DefaultEvaluationContext;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.RuleSubset;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.graph.model.WindupConfigurationModel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.param.DefaultParameterValueStore;
import org.ocpsoft.rewrite.param.ParameterValueStore;

import com.tinkerpop.blueprints.Vertex;

@RunWith(Arquillian.class)
public class RuleIterationOverTypesTest
{
    @Deployment
    @Dependencies({
                @AddonDependency(name = "org.jboss.windup.config:windup-config"),
                @AddonDependency(name = "org.jboss.windup.graph:windup-graph"),
                @AddonDependency(name = "org.jboss.windup.rules.apps:rules-java"),
                @AddonDependency(name = "org.jboss.forge.furnace.container:cdi")
    })
    public static ForgeArchive getDeployment()
    {
        final ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
                    .addBeansXML()
                    .addClasses(
                                IterationOverTypesRuleProvider.class,
                                TestSimple1Model.class,
                                TestSimple2Model.class)
                    .addAsAddonDependencies(
                                AddonDependencyEntry.create("org.jboss.windup.config:windup-config"),
                                AddonDependencyEntry.create("org.jboss.windup.graph:windup-graph"),
                                AddonDependencyEntry.create("org.jboss.windup.rules.apps:rules-java"),
                                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi")
                    );
        return archive;
    }

    @Inject
    private GraphContextFactory factory;

    private DefaultEvaluationContext createEvalContext(GraphRewrite event)
    {
        final DefaultEvaluationContext evaluationContext = new DefaultEvaluationContext();
        final DefaultParameterValueStore values = new DefaultParameterValueStore();
        evaluationContext.put(ParameterValueStore.class, values);
        return evaluationContext;
    }

    @Test
    public void testTypeSelection()
    {
        final File folder = OperatingSystemUtils.createTempDir();
        final GraphContext context = factory.create(folder);
        
        TestSimple1Model vertex = context.getFramed().addVertex(null, TestSimple1Model.class);
        context.getFramed().addVertex(null, TestSimple2Model.class);
        context.getFramed().addVertex(null, TestSimple2Model.class);

        GraphRewrite event = new GraphRewrite(context);
        DefaultEvaluationContext evaluationContext = createEvalContext(event);

        WindupConfigurationModel windupCfg = context.getFramed().addVertex(null, WindupConfigurationModel.class);
        windupCfg.setInputPath("/tmp/testpath");
        windupCfg.setSourceMode(true);

        IterationOverTypesRuleProvider provider = new IterationOverTypesRuleProvider();
        Configuration configuration = provider.getConfiguration(context);

        //this should call perform()
        RuleSubset.evaluate(configuration).perform(event, evaluationContext);
        vertex.asVertex().remove();
        Iterable<Vertex> vertices = context.getFramed().getVertices();
        //this should call otherwise()
        RuleSubset.evaluate(configuration).perform(event, evaluationContext);
        
        Assert.assertEquals(provider.performCounter, 1);
        Assert.assertEquals(provider.otherwiseCounter, 1);
    }

}