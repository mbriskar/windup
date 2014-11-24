package org.jboss.windup.rules.apps.xml.condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public final class NameSpaceAdapter extends XmlAdapter<NameSpaceList, Map<String, String>> {

    
    @Override
    public NameSpaceList marshal(Map<String, String> arg0) throws Exception
    {
        List<NameSpace> namespaces= new ArrayList<NameSpace>();
        NameSpaceList nl = new NameSpaceList();
        for(String key :arg0.keySet()) {
            NameSpace ns = new NameSpace();
            ns.tag=key;
            ns.link=arg0.get(key);
            namespaces.add(ns);
        }
        nl.namespaces=namespaces;
        return nl;
    }

    @Override
    public Map<String, String> unmarshal(NameSpaceList arg0) throws Exception
    {
        Map<String, String> namespaces = new HashMap<String, String>();
        for(NameSpace ns :arg0.namespaces) {
           namespaces.put(ns.tag, ns.link);
        }
        return namespaces;
    }

}