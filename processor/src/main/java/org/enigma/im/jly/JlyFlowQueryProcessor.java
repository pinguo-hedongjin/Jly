package org.enigma.im.jly;

import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;


/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
public class JlyFlowQueryProcessor extends JlyAopProcessor {

    private JlyFlowQuery query;
    private Set<TypeElement> entitySet;

    public JlyFlowQueryProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet,Set<TypeElement> entitySet, JlyFlowQuery query) {
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

        createCount(builder, element);

        builder.addStatement("final $T $N = $T.acquire($N, $N)", JlyConstant.JLY_QUERY_TYPE, JlyConstant.JLY_QUERY_NAME,
                JlyConstant.JLY_QUERY_TYPE, JlyConstant.JLY_SQL_NAME, JlyConstant.JLY_COUNT_NAME);

        bind(builder);

        flow(builder);
    }

    private void flow(MethodSpec.Builder builder) throws Exception {

        TypeName paramsType = getFlowType();

        MethodSpec call = cursor(
                MethodSpec.methodBuilder("call")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addException(Exception.class)
                        .returns(paramsType)
                        .addStatement("$T $N = $N.query($N)", JlyConstant.JLY_CURSOR_TYPE, JlyConstant.JLY_CURSOR_NAME, JlyConstant.JLY_DB_NAME, JlyConstant.JLY_QUERY_NAME)
                        .beginControlFlow("try"))
                .nextControlFlow("finally")
                .addStatement("$N.close()", JlyConstant.JLY_CURSOR_NAME)
                .endControlFlow().build();

        MethodSpec finalize = MethodSpec.methodBuilder("finalize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$N.release()", JlyConstant.JLY_QUERY_NAME)
                .build();

        TypeSpec callable = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Callable.class), paramsType))
                .addMethod(call)
                .addMethod(finalize)
                .build();


        StringBuffer typeList = new StringBuffer();
        StringBuffer valueList = new StringBuffer();
        JlyUtils.forEach(element.getParameters(), (index, size, paramElement) -> {

            String cls = JlyUtils.getClass(paramElement.asType());
            String name = paramElement.getSimpleName().toString();

            typeList.append(cls != null? cls : name + ".getClass()");
            valueList.append(name);

            if (index < size - 1) {
                typeList.append(", ");
                valueList.append(", ");
            }
        });


        builder.addStatement("$T[] $N = $T.getObserveTable(getClass(), $S, new $T[]{$N}, new $T[]{$N})",
                String.class, JlyConstant.JLY_TABLE_NAME, JlyConstant.JLY_LIB_UTILS, element.getSimpleName().toString(),
                Class.class, typeList.toString(), Object.class, valueList.toString());

        builder.addStatement("return $T.createFlowable($N, $N, $L)", JlyConstant.JLY_RXROOM_TYPE, JlyConstant.JLY_DB_NAME, JlyConstant.JLY_TABLE_NAME, callable);
    }

    private void bind(MethodSpec.Builder builder) {
        if (JlyUtils.getMethodParamsLength(element) > 0) {

            builder.addStatement("$T $N = 1", Integer.class, JlyConstant.JLY_OFFSET_NAME);
            for (Element params : element.getParameters()) {
                bind(builder, params);
            }
        }
    }

    /***
     * 解析cursor数据
     * @param builder
     */
    private MethodSpec.Builder cursor(MethodSpec.Builder builder) throws Exception {
        TypeElement entityElement = findEntity();
        if (entityElement == null) {
            return builder;
        }


        // 解析数据
        boolean isList = JlyUtils.isFlowList(element.getReturnType());
        if (isList) {
            builder.addStatement("final $T $N = new $T()", getFlowType(), JlyConstant.JLY_RESULT_NAME, ArrayList.class);
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


        return builder;
    }


    /***
     * 查询返回实体数据类型
     * @return
     */
    private TypeElement findEntity() {
        if (JlyUtils.isFlowList(element.getReturnType())) {
            return findEntity(JlyUtils.getFlowListType(element.getReturnType()));
        } else {
            return findEntity(JlyUtils.getFlowType(element.getReturnType()));
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


    private TypeName getFlowType() {
        if (JlyUtils.isFlowList(element.getReturnType())) {
            return ParameterizedTypeName.get(ClassName.get(List.class), JlyUtils.get(JlyUtils.getFlowListType(element.getReturnType())));
        } else {
            return JlyUtils.get(JlyUtils.getFlowType(element.getReturnType()));
        }
    }

}
