package org.jboss.windup.config.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.ocpsoft.rewrite.config.Operation;




public final class IterationOtherwiseAdapter extends XmlAdapter<IterationOtherwise, Operation> {

    
    
    @Override
    public IterationOtherwise marshal(Operation v) throws Exception
    {
        IterationOtherwise iterationCondition = new IterationOtherwise();
         iterationCondition.op=v;
         return iterationCondition;
    }

    @Override
    public Operation unmarshal(IterationOtherwise v) throws Exception
    {
        return v.op;
    }
    
    

}

@XmlRootElement(name="otherwise")
class IterationOtherwise {
    
    @XmlAnyElement
    Operation op;
    
}