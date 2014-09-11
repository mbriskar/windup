package org.jboss.windup.rules.apps.java.model;

import org.jboss.windup.graph.model.ArchiveModel;
import org.jboss.windup.graph.model.ArchiveType;
import org.jboss.windup.rules.apps.xml.model.XmlFileModel;
import org.jboss.windup.rules.apps.xml.model.XmlResourceModel;

import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("JarArchiveResource")
@ArchiveType(".jar")
public interface JarArchiveModel extends ArchiveModel
{

    @GremlinGroovy("it.out('childArchiveEntry').out('javaClassFacet')")
    public Iterable<JavaClassModel> getJavaClasses();

    @GremlinGroovy("it.sideEffect{x=it}.out('childArchiveEntry').out('javaClassFacet').out('extends', 'imports', 'implements').in('javaClassFacet').in('child').dedup.filter{it!=x}")
    public Iterable<JarArchiveModel> dependsOnArchives();

    @GremlinGroovy("it.sideEffect{x=it}.out('childArchiveEntry').out('javaClassFacet').in('extends', 'imports', 'implements').dedup.in('javaClassFacet').in('child').dedup.filter{it!=x}")
    public Iterable<JarArchiveModel> providesForArchives();

    @GremlinGroovy("it.out('childArchiveEntry').out('xmlResourceFacet')")
    public Iterable<XmlFileModel> getXmlFiles();

    @GremlinGroovy("it.out('childArchiveEntry').out('manifestFacet')")
    public JarManifestModel getJarManifest();
}
