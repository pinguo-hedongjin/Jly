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
public class JlyDeleteProcessor extends JlyAopProcessor {

    private JlyDelete delete;

    public JlyDeleteProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet, JlyDelete delete) {
        super(utils, filer, printer, element, aopSet);
        this.delete = delete;
    }

    @Override
    protected void body(MethodSpec.Builder builder) {
        transaction(builder, () -> handleSql(builder));
    }

    private void handleSql(MethodSpec.Builder builder) {
        createSql(builder, delete.value(), element);

        createCount(builder, element);

        builder.addStatement("$T $N = $N.compileStatement($N)", JlyConstant.JLY_STMT_TYPE, JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_DB_NAME, JlyConstant.JLY_SQL_NAME);

        bind(builder);

        builder.addStatement("$N.executeUpdateDelete()", JlyConstant.JLY_STMT_NAME);
    }

    private void bind(MethodSpec.Builder builder) {
        if (JlyUtils.getMethodParamsLength(element) > 0) {
            builder.addStatement("$T $N = 1", Integer.class, JlyConstant.JLY_OFFSET_NAME);
            for (Element params : element.getParameters()) {
                bind(builder, params);
            }
        }
    }
}
