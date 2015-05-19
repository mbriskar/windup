package org.jboss.windup.rules.apps.xml;

import org.jboss.windup.config.AbstractRuleProvider;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.metadata.MetadataBuilder;
import org.jboss.windup.config.operation.iteration.AbstractIterationOperation;
import org.jboss.windup.config.phase.InitialAnalysisPhase;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.rules.apps.xml.condition.*;
import org.jboss.windup.rules.apps.xml.condition.scan.XMLXpathInterestFactory;
import org.jboss.windup.rules.apps.xml.model.XmlFileModel;
import org.jboss.windup.rules.apps.xml.model.XmlTypeReferenceModel;
import org.jboss.windup.rules.apps.xml.service.XmlFileService;
import org.jboss.windup.util.ExecutionStatistics;
import org.jboss.windup.util.Logging;
import org.jboss.windup.util.xml.LocationAwareContentHandler;
import org.jboss.windup.util.xml.NamespaceMapContext;
import org.jboss.windup.util.xml.XmlUtil;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.param.DefaultParameterStore;
import org.ocpsoft.rewrite.param.Parameter;
import org.ocpsoft.rewrite.param.ParameterStore;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Iterates over all xml files to provide xpath result cache.
 * Created by mbriskar on 5/14/15.
 */
public class XpathResultCacheProvider extends AbstractRuleProvider {

    private static final Logger LOG = Logging.get(XpathResultCacheProvider.class);
    private final XmlFileFunctionResolver xmlFileFunctionResolver;
    private static XPathFactory factory = XPathFactory.newInstance();
    private final XPath xpathEngine;
    private XPathExpression compiledXPath;

    public XpathResultCacheProvider()
    {
        super(MetadataBuilder.forProvider(XpathResultCacheProvider.class)
                .setPhase(InitialAnalysisPhase.class));
        xpathEngine = factory.newXPath();
        final XPathFunctionResolver originalResolver = this.xpathEngine.getXPathFunctionResolver();
        xmlFileFunctionResolver = new XmlFileFunctionResolver(originalResolver);
        this.xpathEngine.setXPathFunctionResolver(xmlFileFunctionResolver);
    }

    @Override
    public Configuration getConfiguration(GraphContext context) {
        return ConfigurationBuilder.begin()
                .addRule()
                .when(Query.fromType(XmlFileModel.class))
                .perform(new AbstractIterationOperation<XmlFileModel>()
                {
                    @Override
                    public void perform(GraphRewrite event, EvaluationContext context, XmlFileModel payload)
                    {
                        XmlFileService xmlFileService = new XmlFileService(event.getGraphContext());
                        Document document = xmlFileService.loadDocumentQuiet(payload);
                        ExecutionStatistics.get().begin("XpathResultCacheProvider.createXpathCache");
                        final List<WindupVertexFrame> resultLocations = new ArrayList<WindupVertexFrame>();
                        final GraphContext graphContext = event.getGraphContext();
                        GraphService<XmlFileModel> xmlResourceService = new GraphService<XmlFileModel>(graphContext,
                                XmlFileModel.class);
                        for(Map.Entry<String,Map<String, String>> xpathEntry: XMLXpathInterestFactory.registeredXpaths().entrySet()) {
                            if (document != null)
                            {
                                NamespaceMapContext nsContext = new NamespaceMapContext(xpathEntry.getValue());
                                xpathEngine.setNamespaceContext(nsContext);
                                XPathExpression compiledXPath = null;
                                try {
                                    compiledXPath = xpathEngine.compile(xpathEntry.getKey());

                                } catch (Exception e) {
                                    String message = e.getMessage();

                                    // brutal hack to try to get a reasonable error message (ugly, but it seems to work)
                                    if (message == null && e.getCause() != null && e.getCause().getMessage() != null) {
                                        message = e.getCause().getMessage();
                                    }
                                    LOG.severe("Condition: " + this + " failed to run, as the following xpath was uncompilable: " + xpathEntry.getKey()
                                            + " due to: "
                                            + message);
                                    continue;
                                }
                                final ParameterStore store = DefaultParameterStore.getInstance(context);

                                final XmlFileParameterMatchCache paramMatchCache = new XmlFileParameterMatchCache();
                                //register windup specific functions that are going to be triggered by xpath engine if they occur in xpath
                                xmlFileFunctionResolver.registerFunction(XmlFile.WINDUP_NS_URI, "startFrame", new XmlFileStartFrameXPathFunction(paramMatchCache));
                                xmlFileFunctionResolver.registerFunction(XmlFile.WINDUP_NS_URI, "evaluate", new XmlFileEvaluateXPathFunction());
                                xmlFileFunctionResolver.registerFunction(XmlFile.WINDUP_NS_URI, "matches", new XmlFileMatchesXPathFunction(context, store,
                                        paramMatchCache, event));
                                xmlFileFunctionResolver.registerFunction(XmlFile.WINDUP_NS_URI, "persist", new XpathCachePersistFunction(event, context, payload,
                                        store, paramMatchCache, resultLocations, xpathEntry.getKey()));
                                /**
                                 * This actually does the work.
                                 */

                                XmlUtil.xpathNodeList(document, compiledXPath);
                            }
                        }
                    }


                });
    }

    final class XpathCachePersistFunction implements XPathFunction
    {
        private final GraphRewrite event;
        private final EvaluationContext context;
        private final XmlFileModel xml;
        private final ParameterStore store;
        private final XmlFileParameterMatchCache paramMatchCache;
        private final List<WindupVertexFrame> resultLocations;
        private final String xpathString;

        XpathCachePersistFunction(GraphRewrite event, EvaluationContext context, XmlFileModel xml,
                                    ParameterStore store,
                                    XmlFileParameterMatchCache paramMatchCache, List<WindupVertexFrame> resultLocations, String xpathString)
        {
            this.event = event;
            this.context = context;
            this.xml = xml;
            this.store = store;
            this.paramMatchCache = paramMatchCache;
            this.resultLocations = resultLocations;
            this.xpathString = xpathString;
        }

        @Override
        public Object evaluate(@SuppressWarnings("rawtypes") List args) throws XPathFunctionException
        {
            int frameIdx = ((Double) args.get(0)).intValue();
            NodeList arg1 = (NodeList) args.get(1);
            String nodeText = XmlUtil.nodeListToString(arg1);
            LOG.fine("persist(" + frameIdx + ", " + nodeText + ")");

            for (int i = 0; i < arg1.getLength(); i++)
            {
                Node node = arg1.item(i);
                int lineNumber = (int) node.getUserData(
                        LocationAwareContentHandler.LINE_NUMBER_KEY_NAME);
                int columnNumber = (int) node.getUserData(
                        LocationAwareContentHandler.COLUMN_NUMBER_KEY_NAME);

                GraphService<XmlTypeReferenceModel> fileLocationService = new GraphService<XmlTypeReferenceModel>(
                        event.getGraphContext(),
                        XmlTypeReferenceModel.class);
                XmlTypeReferenceModel fileLocation = fileLocationService.create();
                String sourceSnippit = XmlUtil.nodeToString(node);
                fileLocation.setSourceSnippit(sourceSnippit);
                fileLocation.setLineNumber(lineNumber);
                fileLocation.setColumnNumber(columnNumber);
                fileLocation.setLength(node.toString().length());
                fileLocation.setFile(xml);
               /* for (Map.Entry<String, String> entry : paramMatchCache.getVariables(frameIdx).entrySet())
                {
                    Parameter<?> param = store.get(entry.getKey());
                    String value = entry.getValue();
                    xpathString.replaceAll("{ " + param.getName()+ "}", value);
                }*/
                fileLocation.setXpath(xpathString);
                resultLocations.add(fileLocation);
            }

            return true;
        }
    }

}
