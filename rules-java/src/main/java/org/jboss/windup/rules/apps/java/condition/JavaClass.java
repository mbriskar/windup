package org.jboss.windup.rules.apps.java.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.forge.furnace.util.Assert;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.condition.GraphCondition;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.config.query.QueryBuilderWith;
import org.jboss.windup.config.query.QueryGremlinCriterion;
import org.jboss.windup.config.query.QueryPropertyComparisonType;
import org.jboss.windup.reporting.model.FileReferenceModel;
import org.jboss.windup.rules.apps.java.model.JavaClassModel;
import org.jboss.windup.rules.apps.java.model.JavaSourceFileModel;
import org.jboss.windup.rules.apps.java.scan.ast.JavaTypeReferenceModel;
import org.jboss.windup.rules.apps.java.scan.ast.TypeInterestFactory;
import org.jboss.windup.rules.apps.java.scan.ast.TypeReferenceLocation;
import org.ocpsoft.rewrite.config.Condition;
import org.ocpsoft.rewrite.config.ConditionBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;

import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;

/**
 * {@link GraphCondition} that matches Vertices in the graph based upon the provided parameters.
 */
@XmlRootElement(name="javaclass")
public class JavaClass extends GraphCondition implements JavaClassBuilder, JavaClassBuilderAt, JavaClassBuilderInFile
{
    @XmlAttribute(name = "references")
    private final String regex;
    @XmlElement(name="location")
    private List<TypeReferenceLocation> locations = Collections.emptyList();
    @XmlAttribute(name="as")
    private String variable = Iteration.DEFAULT_VARIABLE_LIST_STRING;
    @XmlAttribute(name="in")
    private String typeFilterRegex;

    private JavaClass(String regex)
    {
        this.regex = regex;
        TypeInterestFactory.registerInterest(regex);
    }
    
    public JavaClass() {
        regex="";
    }

    /**
     * Create a new {@link JavaClass} {@link Condition} based upon the provided Java regular expression.
     */
    public static JavaClassBuilder references(String regex)
    {
        return new JavaClass(regex);
    }

    /**
     * Create a new {@link JavaClass} {@link Condition} based upon the provided Java regular expression.
     */
    public static JavaClassBuilderReferences from(String inputVarName)
    {
        return new JavaClassBuilderReferences(inputVarName);
    }

    /**
     * Specify a Java type pattern regex for which this condition should match.
     */
    public JavaClassBuilderInFile inType(String regex)
    {
        this.typeFilterRegex = regex;
        return this;
    }

    /**
     * Only match if the TypeReference is at the specified location within the file.
     */
    @Override
    public JavaClassBuilderAt at(TypeReferenceLocation... locations)
    {
        if (locations != null)
            this.locations = Arrays.asList(locations);
        return this;
    }

    /**
     * Optionally specify the variable name to use for the output of this condition
     */
    @Override
    public ConditionBuilder as(String variable)
    {
        Assert.notNull(variable, "Variable name must not be null.");
        this.variable = variable;
        return this;
    }

    @Override
    public boolean evaluate(GraphRewrite event, EvaluationContext context)
    {
        QueryBuilderWith query;
        if (getInputVariablesName() != null && !getInputVariablesName().equals(""))
        {
            query = Query.from(getInputVariablesName());
        }
        else
        {
            query = Query.find(JavaTypeReferenceModel.class);
        }
        query.withProperty(JavaTypeReferenceModel.SOURCE_SNIPPIT, QueryPropertyComparisonType.REGEX, regex);
        if (typeFilterRegex != null)
        {
            QueryGremlinCriterion inFileWithName = new QueryGremlinCriterion()
            {
                @Override
                public void query(GraphRewrite event, GremlinPipeline<Vertex, Vertex> pipeline)
                {
                    Predicate regexPredicate = new Predicate()
                    {
                        @Override
                        public boolean evaluate(Object first, Object second)
                        {
                            return ((String) first).matches((String) second);
                        }

                    };
                    pipeline.as("result").out(FileReferenceModel.FILE_MODEL)
                                .out(JavaSourceFileModel.JAVA_CLASS_MODEL)
                                .has(JavaClassModel.PROPERTY_QUALIFIED_NAME, regexPredicate, typeFilterRegex)
                                .back("result");
                }
            };
            query.piped(inFileWithName);
        }
        if (!locations.isEmpty())
            query.withProperty(JavaTypeReferenceModel.REFERENCE_TYPE, locations);
        return query.as(variable).evaluate(event, context);
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("JavaClass");
        if (typeFilterRegex != null)
        {
            builder.append(".inType(" + typeFilterRegex + ")");
        }
        if (regex != null)
        {
            builder.append(".references(" + regex + ")");
        }
        if (!locations.isEmpty())
        {
            builder.append(".at(" + locations + ")");
        }
        builder.append(".as(" + variable + ")");
        return builder.toString();
    }
}
