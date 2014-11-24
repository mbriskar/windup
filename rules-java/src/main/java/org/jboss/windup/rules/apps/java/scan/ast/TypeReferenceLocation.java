package org.jboss.windup.rules.apps.java.scan.ast;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Designates a location where a given {@link JavaTypeReferenceModel} was found in a Java source file.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@XmlType(name = "location")
@XmlEnum
public enum TypeReferenceLocation
{
    IMPORT,
    TYPE,
    METHOD,
    INHERITANCE,
    CONSTRUCTOR_CALL,
    METHOD_CALL,
    METHOD_PARAMETER,
    ANNOTATION,
    RETURN_TYPE,
    INSTANCE_OF,
    THROWS_METHOD_DECLARATION,
    THROW_STATEMENT,
    CATCH_EXCEPTION_STATEMENT,
    FIELD_DECLARATION,
    VARIABLE_DECLARATION,
    IMPLEMENTS_TYPE,
    EXTENDS_TYPE
}
