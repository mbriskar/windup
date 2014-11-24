package org.jboss.windup.rules.apps.xml.condition;

import java.util.List;

import javax.xml.bind.annotation.XmlAnyElement;

public class NameSpaceList
{   
    @XmlAnyElement
    public List<NameSpace> namespaces;
}
