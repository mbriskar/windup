package org.jboss.windup.rules.apps.xml.condition.scan;

import org.apache.commons.collections.map.LRUMap;
import org.jboss.windup.rules.apps.xml.condition.XmlFile;
import org.jboss.windup.rules.apps.xml.condition.XmlFileFunctionResolver;
import org.jboss.windup.rules.apps.xml.condition.XmlFileXPathTransformer;
import org.jboss.windup.util.ExecutionStatistics;
import org.jboss.windup.util.Logging;
import org.jboss.windup.util.xml.NamespaceMapContext;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunctionResolver;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by mbriskar on 5/14/15.
 */
public class XMLXpathInterestFactory {

    private static Logger LOG = Logging.get(XMLXpathInterestFactory.class);

    // Keep track of each pattern, as well as an identifier of who gave the pattern to us (so that we can update it)
    private static Map<XPathExpression,String> xpaths = new HashMap<>();
    private static XPathFactory factory = XPathFactory.newInstance();
    private static final XPath xpathEngine;

static {
    xpathEngine = factory.newXPath();
}
    /**
     * Register a regex pattern to filter interest in certain Java types.
     */
    public static boolean registerInterest(String xpath,  Map<String, String> namespaces)
    {
        namespaces.put(XmlFile.WINDUP_NS_PREFIX, XmlFile.WINDUP_NS_URI);
        XPathExpression compiledXPath = null;
        if (xpath == null)
        {
            NamespaceMapContext nsContext = new NamespaceMapContext(namespaces);
            xpathEngine.setNamespaceContext(nsContext);
            String xpathStringWithParameterFunctions = XmlFileXPathTransformer.transformXPath(xpath);
            try
            {
                compiledXPath = xpathEngine.compile(xpathStringWithParameterFunctions);
            }
            catch (Exception e)
            {
                String message = e.getMessage();

                // brutal hack to try to get a reasonable error message (ugly, but it seems to work)
                if (message == null && e.getCause() != null && e.getCause().getMessage() != null)
                {
                    message = e.getCause().getMessage();
                }
                LOG.severe("XPathInterestFactory: failed to run, as the following xpath was uncompilable: " + xpath
                        + " (compiled contents: " + xpathStringWithParameterFunctions + ") due to: "
                        + message);
                return false;
            }
        }
        if(compiledXPath != null) {
            xpaths.put(compiledXPath,xpath);
            return true;
        } else {
            return false;
        }

    }

    private static Boolean checkCacheForMatches(XPathExpression xpath)
    {
        Boolean cachedResult = (Boolean) xpaths.containsKey(xpath);
        return cachedResult;
    }


    public static boolean matchesAny(String xpath)
    {
        ExecutionStatistics.get().begin("XPathInterestFactory.matchesAny(text)");
        boolean result = xpaths.containsValue(xpath);
        ExecutionStatistics.get().end("XPathInterestFactory.matchesAny(text)");
        return result;
    }

    public static Iterable<XPathExpression> registeredXpaths() {
        return new Iterable<XPathExpression>() {

            @Override
            public Iterator<XPathExpression> iterator() {
                return xpaths.keySet().iterator();
            }
        };
    }

}
