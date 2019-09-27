package org.enigma.im.jly;

import com.squareup.javapoet.ClassName;
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
public class JlyFtsQueryProcessor extends JlyAopProcessor {

    private JlyFtsQuery query;
    private Set<TypeElement> entitySet;

    public JlyFtsQueryProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet, Set<TypeElement> entitySet, JlyFtsQuery query) {
        super(utils, filer, printer, element, aopSet);
        this.entitySet = entitySet;
        this.query = query;
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

        builder.addStatement("$T $N = $N.getOpenHelper().getWritableDatabase().query($N)", JlyConstant.JLY_CURSOR_TYPE, JlyConstant.JLY_CURSOR_NAME, JlyConstant.JLY_DB_NAME, JlyConstant.JLY_SQL_NAME);
        builder.beginControlFlow("try");

        cursor(builder);

        builder.nextControlFlow("finally");
        builder.addStatement("$N.close()", JlyConstant.JLY_CURSOR_NAME);
        builder.endControlFlow();
    }


    private void replace(MethodSpec.Builder builder) {
        if (JlyUtils.getMethodParamsLength(element) > 0) {
            for (Element params : element.getParameters()) {
                builder.addStatement("$N = $N.replaceFirst(\"\\\\?\", $N + \"\")",
                        JlyConstant.JLY_SQL_NAME, JlyConstant.JLY_SQL_NAME, params.getSimpleName());
            }
        }
    }

    /***
     * 解析cursor数据
     * @param builder
     */
    private void cursor(MethodSpec.Builder builder) throws Exception {
        TypeElement entityElement = findEntity();
        if (entityElement == null) {
            return;
        }

        // 解析数据
        boolean isList = JlyUtils.isList(element.getReturnType());
        if (isList) {
            builder.addStatement("final $T $N = new $T()", element.getReturnType(), JlyConstant.JLY_RESULT_NAME, ArrayList.class);
            builder.beginControlFlow("while($N.moveToNext())", JlyConstant.JLY_CURSOR_NAME);

            ClassName parseEntity = ClassName.get(JlyUtils.getPackageName(utils, entityElement), JlyUtils.getClassName(entityElement) + JlyConstant.JLY_PARSE_SUFFIX);
            builder.addStatement("$N.add(new $T().$N($N))", JlyConstant.JLY_RESULT_NAME, parseEntity, JlyConstant.JLY_PARSE_METHOD, JlyConstant.JLY_CURSOR_NAME);

            builder.endControlFlow();
            builder.addStatement("return $N", JlyConstant.JLY_RESULT_NAME);

        } else {
            builder.beginControlFlow("if($N.moveToFirst())", JlyConstant.JLY_CURSOR_NAME);

            ClassName parseEntity = ClassName.get(JlyUtils.getPackageName(utils, entityElement), JlyUtils.getClassName(entityElement) + JlyConstant.JLY_PARSE_SUFFIX);
            builder.addStatement("return new $T().$N($N)", parseEntity, JlyConstant.JLY_PARSE_METHOD, JlyConstant.JLY_CURSOR_NAME);

            builder.endControlFlow();
            builder.addStatement("return null");

        }


    }

    /***
     * 查询返回实体数据类型
     * @return
     */
    private TypeElement findEntity() {
        if (JlyUtils.isList(element.getReturnType())) {
            return findEntity(JlyUtils.getListType(element.getReturnType()));
        } else {
            return findEntity(element.getReturnType().toString());
        }
    }

    private TypeElement findEntity(String canonicalName) {
        for (TypeElement element : entitySet) {
            if (element.getQualifiedName().toString().equals(canonicalName)) {
                return element;
            }
        }

        return null;
    }
}
