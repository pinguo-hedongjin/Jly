package org.enigma.im.jly;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.util.Set;


/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
public class JlyAopProcessor extends BaseProcessor<MethodSpec> {

    protected ExecutableElement element;
    protected Set<TypeElement> aopSet;

    public JlyAopProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet) {
        super(utils, filer, printer);
        this.element = element;
        this.aopSet = aopSet;
    }

    @Override
    public final MethodSpec process() {
        final MethodSpec.Builder builder = JlyUtils.buildMethodHead(element);

        aop(builder, () -> body(builder));

        return builder.build();
    }

    private void aop(MethodSpec.Builder builder, Runnable code) {
        if (aopSet == null || aopSet.isEmpty()) {
            code.run();
            return;
        }

        final String params = JlyUtils.getMethodParams(element);
        JlyUtils.forEach(aopSet, (index, size, type) -> {
            String methodName = JlyConstant.JLY_AOP_PREFIX + index;
            ClassName className = ClassName.get(JlyUtils.getPackageName(utils, type), JlyUtils.getClassName(type));
            builder.addStatement("$T $N = new $T()", className, methodName, className);
            builder.beginControlFlow("if($N.isPointcut(this, $S, new Object[]{$N}))", methodName, element.getSimpleName().toString(), params);
            builder.addStatement("$N.before(this, $S, new Object[]{$N})", methodName, element.getSimpleName().toString(), params);
            builder.endControlFlow();
        });

        builder.beginControlFlow("try");
        code.run();
        builder.nextControlFlow("finally");

        JlyUtils.forEach(aopSet, (index, size, type) -> {
            String methodName = JlyConstant.JLY_AOP_PREFIX + index;
            builder.beginControlFlow("if($N.isPointcut(this, $S, new Object[]{$N}))", methodName, element.getSimpleName().toString(), params);
            builder.addStatement("$N.after(this, $S, new Object[]{$N})", methodName, element.getSimpleName().toString(), params);
            builder.endControlFlow();
        });

        builder.endControlFlow();
    }


    protected void body(MethodSpec.Builder builder) {

        if (element.getReturnType().getKind() == TypeKind.VOID) {
            builder.addStatement("$N.$N($N)", JlyConstant.JLY_DAO_NAME, element.getSimpleName().toString(), JlyUtils.getMethodParams(element));
        } else {
            builder.addStatement("return $N.$N($N)", JlyConstant.JLY_DAO_NAME, element.getSimpleName().toString(), JlyUtils.getMethodParams(element));
        }

    }

    protected void transaction(MethodSpec.Builder builder, Runnable code) {
        builder.addStatement("$N.beginTransaction()", JlyConstant.JLY_DB_NAME);
        builder.beginControlFlow("try");

        code.run();

        builder.addStatement("$N.setTransactionSuccessful()", JlyConstant.JLY_DB_NAME);
        builder.nextControlFlow("finally");
        builder.addStatement("$N.endTransaction()", JlyConstant.JLY_DB_NAME);
        builder.endControlFlow();
    }

    public void createSql(MethodSpec.Builder builder, String sql, ExecutableElement element) {
        boolean hasListOrAction = false;
        for (Element paramElement : element.getParameters()) {
            if (JlyUtils.containJlyObserveTable(paramElement))continue;

            String name = paramElement.getSimpleName().toString();
            if (JlyUtils.containJlyActionTable(paramElement) || JlyUtils.isList(paramElement) || JlyUtils.isPrimitiveArray(paramElement)) {
                hasListOrAction = true;
            } else {
                sql = sql.replaceFirst(":\\s*" + name, "?");
            }
        }

        builder.addStatement("$T $N = $S", String.class, JlyConstant.JLY_SQL_NAME, sql);

        if (hasListOrAction) {
            for (Element paramElement : element.getParameters()) {

                String name = paramElement.getSimpleName().toString();
                if (JlyUtils.containJlyActionTable(paramElement)) {
                    String regex = ":\\s*" + name;
                    builder.addStatement("$N = $N.replaceAll($S, $N)",
                            JlyConstant.JLY_SQL_NAME, JlyConstant.JLY_SQL_NAME, regex, name);
                    continue;
                }

                if (JlyUtils.isList(paramElement)) {
                    String regex = ":\\s*" + name;
                    builder.addStatement("$N = $N.replaceAll($S, $T.formatList($N.size()))",
                            JlyConstant.JLY_SQL_NAME, JlyConstant.JLY_SQL_NAME, regex, JlyConstant.JLY_LIB_UTILS, name);
                    continue;
                }

                if (JlyUtils.isPrimitiveArray(paramElement) && !JlyUtils.isByteArray(paramElement)) {
                    String regex = ":\\s*" + name;
                    builder.addStatement("$N = $N.replaceAll($S, $T.formatList($N.length))",
                            JlyConstant.JLY_SQL_NAME, JlyConstant.JLY_SQL_NAME, regex, JlyConstant.JLY_LIB_UTILS, name);
                }



            }
        }
    }

    protected void createCount(MethodSpec.Builder builder, ExecutableElement element) {
        if (JlyUtils.getMethodParamsLength(element) > 0) {
            StringBuffer countBuffer = new StringBuffer("$T $N = 0");

            for (Element paramElement : element.getParameters()) {
                if (JlyUtils.containJlyActionTable(paramElement)) continue;
                if (JlyUtils.containJlyObserveTable(paramElement)) continue;

                String name = paramElement.getSimpleName().toString();
                if (JlyUtils.isList(paramElement)) {
                    countBuffer.append("+" + name + ".size()");
                } else if (JlyUtils.isPrimitiveArray(paramElement) && !JlyUtils.isByteArray(paramElement)) {
                    countBuffer.append("+" + name + ".length");
                } else {
                    countBuffer.append("+1");
                }
            }

            builder.addStatement(countBuffer.toString(), Integer.class, JlyConstant.JLY_COUNT_NAME);
        } else {
            builder.addStatement("$T $N = 0", Integer.class, JlyConstant.JLY_COUNT_NAME);
        }
    }

    protected void bind(MethodSpec.Builder builder, Element params) {
        if (JlyUtils.containJlyActionTable(params) || JlyUtils.containJlyObserveTable(params) ) return;

        String name = params.getSimpleName().toString();
        TypeKind type = params.asType().getKind();
        if (type.isPrimitive()) {
            switch (type) {
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                case CHAR:
                    JlyUtils.bindLong(builder, name);
                    break;
                case BOOLEAN:
                    JlyUtils.bindBoolean(builder, name);
                    break;
                case FLOAT:
                case DOUBLE:
                    JlyUtils.bindDouble(builder, name);
                    break;
            }

        } else if (JlyUtils.isByteArray(params)) {
            JlyUtils.bindBlob(builder, name);
        } else if (JlyUtils.isPrimitiveArray(params)) {
            JlyUtils.bindArray(builder, params);
        } else if (JlyUtils.isString(params)) {
            JlyUtils.bindString(builder, name);
        } else if (JlyUtils.isList(params)) {
            JlyUtils.bindList(builder, params);
        }
    }

}
