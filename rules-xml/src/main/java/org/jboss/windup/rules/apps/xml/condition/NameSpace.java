package org.jboss.windup.rules.apps.xml.condition;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="namespace")
public class NameSpace
{
    @XmlAttribute(name="tag")
    public String tag;
    @XmlAttribute(name="link")
    public String link;
}
