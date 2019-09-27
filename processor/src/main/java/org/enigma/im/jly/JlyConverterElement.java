package org.enigma.im.jly;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * author:  hedongjin
 * date:  2019-07-04
 * description: Please contact me if you have any questions
 */
public class JlyConverterElement {
    public final int dbType;
    public final TypeElement typeElement;
    public final ExecutableElement toDbElement;
    public final ExecutableElement toOuterElement;

    public JlyConverterElement(int dbType, TypeElement typeElement, ExecutableElement toDbElement, ExecutableElement toOuterElement) {
        this.dbType = dbType;
        this.typeElement = typeElement;
        this.toDbElement = toDbElement;
        this.toOuterElement = toOuterElement;
    }
}
