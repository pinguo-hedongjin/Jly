package org.enigma.im.jly;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Set;


/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
public class JlyExecProcessor extends JlyAopProcessor {

    public JlyExecProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet) {
        super(utils, filer, printer, element, aopSet);
    }

    @Override
    protected void body(MethodSpec.Builder builder) {
        transaction(builder, () -> handleSql(builder));
    }

    private void handleSql(MethodSpec.Builder builder) {
        if (JlyUtils.getMethodParamsLength(element) > 0) {

            for (Element params : element.getParameters()) {
                builder.addStatement("$N.getOpenHelper().getWritableDatabase().execSQL($N)", JlyConstant.JLY_DB_NAME, params.getSimpleName().toString());
            }
        }

    }
}
