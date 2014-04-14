package org.jboss.windup.engine.visitor.reporter.html.renderer;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.jboss.windup.engine.WindupContext;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.engine.visitor.reporter.html.model.ApplicationReport;
import org.jboss.windup.engine.visitor.reporter.html.model.ArchiveReport;
import org.jboss.windup.engine.visitor.reporter.html.model.Level;
import org.jboss.windup.engine.visitor.reporter.html.model.ArchiveReport.ResourceReportRow;
import org.jboss.windup.engine.visitor.reporter.html.model.Link;
import org.jboss.windup.engine.visitor.reporter.html.model.Tag;
import org.jboss.windup.graph.dao.ApplicationReferenceDaoBean;
import org.jboss.windup.graph.model.meta.ApplicationReference;
import org.jboss.windup.graph.model.meta.JarManifest;
import org.jboss.windup.graph.model.resource.ArchiveEntryResource;
import org.jboss.windup.graph.model.resource.ArchiveResource;
import org.jboss.windup.graph.model.resource.JavaClass;
import org.jboss.windup.graph.model.resource.XmlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class ApplicationReportRenderer extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationReportRenderer.class);
	
	@Inject
	private WindupContext context;
	
	@Inject
	private ApplicationReferenceDaoBean appRefDao;

	
	
	private final Configuration cfg;
	
	public ApplicationReportRenderer() {
		cfg = new Configuration();
        cfg.setTemplateUpdateDelay(500);
        cfg.setClassForTemplateLoading(this.getClass(), "/");
	}
	
	
	@Override
	public void run() {
		ApplicationReport applicationReport = new ApplicationReport();
		
		for(ApplicationReference app : appRefDao.getAll()) {
			ArchiveResource reference = app.getArchive();
			applicationReport.setApplicationName(reference.getArchiveName());
			
			recurseArchive(applicationReport, app.getArchive());
		}
		
		
		
		
		try {
			Template template = cfg.getTemplate("/reports/templates/application.ftl");
			
			java.util.Map<String, Object> objects = new HashMap<String, Object>();
			objects.put("application", applicationReport);
			
			File runDirectory = context.getRunDirectory();
			File archiveReportDirectory = new File(runDirectory, "applications");
			File archiveDirectory = new File(archiveReportDirectory, "application");
			FileUtils.forceMkdir(archiveDirectory);
			File archiveReport = new File(archiveDirectory, "index.html");
			
			template.process(objects, new FileWriter(archiveReport));
			
			LOG.info("Wrote report: "+archiveReport.getAbsolutePath());
			
		} catch (Exception e) {
			throw new RuntimeException("Exception writing report.", e);
		}
	}
	
	protected void recurseArchive(ApplicationReport report, ArchiveResource resource) {
		ArchiveReport archiveReport = new ArchiveReport();
		archiveReport.setApplicationPath(resource.getArchiveName());
		
		for(ArchiveEntryResource entry : resource.getChildrenArchiveEntries()) {
			//check to see about facets.
			archiveReport.getResources().add(processEntry(entry));
		}
		
		for(ArchiveResource childResource : resource.getChildrenArchive()) {
			recurseArchive(report, childResource);
		}
		
		report.getArchives().add(archiveReport);
	}
	
	protected ResourceReportRow processEntry(ArchiveEntryResource entry) {
		ResourceReportRow reportRow = new ResourceReportRow();
		
		//see if the resource is a java class...
		{
			Iterable<Vertex> edge = entry.asVertex().getVertices(Direction.OUT, "javaClassFacet");
			if(edge.iterator().hasNext()) {
				for(Vertex v : edge) {
					JavaClass javaClass = context.getGraphContext().getFramed().frame(v, JavaClass.class);
					reportRow.setResourceLink(new Link("#", javaClass.getQualifiedName()));
					reportRow.getTechnologyTags().add(new Tag("Java", Level.PRIMARY));
					
					return reportRow;
				}
			}
		}

		{
			Iterable<Vertex> edge = entry.asVertex().getVertices(Direction.OUT, "xmlResourceFacet");
			if(edge.iterator().hasNext()) {
				for(Vertex v : edge) {
					XmlResource resource = context.getGraphContext().getFramed().frame(v, XmlResource.class);
					reportRow.setResourceLink(new Link("#", entry.getArchiveEntry()));
					reportRow.getTechnologyTags().add(new Tag("XML", Level.PRIMARY));
					
					return reportRow;
				}
			}
		}
		
		{
			Iterable<Vertex> edge = entry.asVertex().getVertices(Direction.OUT, "manifestFacet");
			if(edge.iterator().hasNext()) {
				for(Vertex v : edge) {
					JarManifest resource = context.getGraphContext().getFramed().frame(v, JarManifest.class);
					reportRow.setResourceLink(new Link("#", entry.getArchiveEntry()));
					reportRow.getTechnologyTags().add(new Tag("Manifest", Level.PRIMARY));
					
					return reportRow;
				}
			}
		}
		
		
		
		
		reportRow.setResourceLink(new Link("#", entry.getArchiveEntry()));
		reportRow.getIssueTags().add(new Tag("Unknown Type", Level.WARNING));
			
		return reportRow;
		
	}
	
}