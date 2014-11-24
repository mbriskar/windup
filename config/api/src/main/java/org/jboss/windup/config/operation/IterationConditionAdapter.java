package org.jboss.windup.config.operation;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.ocpsoft.rewrite.config.Condition;


public final class IterationConditionAdapter extends XmlAdapter<IterationCondition, Condition> {

    
    
    @Override
    public IterationCondition marshal(Condition v) throws Exception
    {
        IterationCondition iterationCondition = new IterationCondition();
         iterationCondition.c=v;
         return iterationCondition;
    }

    @Override
    public Condition unmarshal(IterationCondition v) throws Exception
    {
        return v.c;
    }
    
    

}

@XmlRootElement(name="when")
class IterationCondition {
    
    @XmlAnyElement
    Condition c;
    
}