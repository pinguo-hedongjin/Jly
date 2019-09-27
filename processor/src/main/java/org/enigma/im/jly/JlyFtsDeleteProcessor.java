package org.enigma.im.jly;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
public class JlyFtsDeleteProcessor extends JlyAopProcessor {

    private JlyFtsDelete query;

    public JlyFtsDeleteProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet, JlyFtsDelete delete) {
        super(utils, filer, printer, element, aopSet);
        this.query = delete;
    }

    @Override
    protected void body(MethodSpec.Builder builder) {
        try {
            handleSql(builder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSql(MethodSpec.Builder builder) throws Exception {
        createSql(builder, query.value(), element);

        replace(builder);

        builder.addStatement("$N.getOpenHelper().getWritableDatabase().execSQL($N)", JlyConstant.JLY_DB_NAME, JlyConstant.JLY_SQL_NAME);
    }


    private void replace(MethodSpec.Builder builder) {
        if (JlyUtils.getMethodParamsLength(element) > 0) {
            for (Element params : element.getParameters()) {
                builder.addStatement("$N = $N.replaceFirst(\"\\\\?\", $N + \"\")",
                        JlyConstant.JLY_SQL_NAME, JlyConstant.JLY_SQL_NAME, params.getSimpleName());
            }
        }
    }

}
