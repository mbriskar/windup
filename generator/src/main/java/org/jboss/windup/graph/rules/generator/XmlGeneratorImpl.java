package org.jboss.windup.graph.rules.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.jboss.windup.config.WindupRuleProvider;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.config.selectors.XmlGenerator;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.reporting.config.Classification;
import org.jboss.windup.reporting.config.Hint;
import org.jboss.windup.rules.apps.java.condition.JavaClass;
import org.jboss.windup.rules.apps.xml.condition.NameSpace;
import org.jboss.windup.rules.apps.xml.condition.XmlFile;
import org.jboss.windup.rules.apps.xml.operation.xslt.XSLTTransformation;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.ConfigurationRuleBuilder;
import org.ocpsoft.rewrite.config.PerformA;
import org.ocpsoft.rewrite.config.RuleBuilder;
import org.ocpsoft.rewrite.config.WhenA;


public class XmlGeneratorImpl implements XmlGenerator
{

    @Override
    public void process(WindupRuleProvider provider, GraphContext context)
    {
        Configuration cfg = provider.getConfiguration(context);
        String fileName="/tmp/transformed/" + provider.getClass().getName().split("_\\$\\$")[0] + ".windup.xml";
        File file = new File(fileName);
        JAXBContext jaxbContext;
        try
        {
            jaxbContext = JAXBContext.newInstance(ConfigurationBuilder.class,WhenA.class,XSLTTransformation.class,NameSpace.class,PerformA.class,XmlFile.class,Iteration.class,JavaClass.class,Classification.class,Hint.class,RuleBuilder.class,ConfigurationRuleBuilder.class);
        
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
 
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
 
        jaxbMarshaller.marshal(cfg, file);
        jaxbMarshaller.marshal(cfg, System.out);
        
        File tempFile = new File("/tmp/transformed/" + "myTempFile" +".xml");

        BufferedReader reader = new BufferedReader(new FileReader(file));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String lineToRemove = "bbb";
        String currentLine;
        
        while((currentLine = reader.readLine()) != null) {
            // trim newline when comparing with lineToRemove
            String trimmedLine = currentLine.trim();
            
            if(trimmedLine.contains("?xml")) {
                writer.write(currentLine + System.getProperty("line.separator"));
                writer.write("<ruleset xmlns=\"http://windup.jboss.org/v1/xml\" id=\" " + provider.getClass().getSimpleName().split("_\\$\\$")[0] + "\">" + System.getProperty("line.separator"));
               
                continue;
            }
            if(trimmedLine.contains("<namespaces>")) continue;
            if(trimmedLine.contains("<condition>")) trimmedLine.replace("<condition>", "<when>");
            if(trimmedLine.contains("</condition>")) trimmedLine.replace("</condition>", "</when>");
            if(trimmedLine.contains("</namespaces>")) continue;
            if(trimmedLine.contains("<operationOtherwise/>")) continue;
            if(trimmedLine.contains("<condition/>")) continue;
            if(trimmedLine.contains("<wrapped>")) continue;
            if(trimmedLine.contains("<links/>")) continue;
            if(trimmedLine.contains("</wrapped>")) continue;
            if(trimmedLine.contains("<rules/>")) continue;
            if(trimmedLine.contains("<configurationRuleBuilder>")) continue;
            if(trimmedLine.contains("</configurationRuleBuilder>")) continue;
            if(trimmedLine.contains("<defaultOperationBuilderInternal>")) continue;
            if(trimmedLine.contains("</defaultOperationBuilderInternal>")) continue;
            if(trimmedLine.contains("<iteration/>")) continue;
            if(trimmedLine.contains("<namespaces/>")) continue;
            if(trimmedLine.contains("<defaultConditionBuilderInternal>")) continue;
            if(trimmedLine.contains("</defaultConditionBuilderInternal>")) continue;
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.write("</ruleset>");
        writer.close(); 
        reader.close(); 
        boolean successful = tempFile.renameTo(file);
        }
        catch (JAXBException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

}
