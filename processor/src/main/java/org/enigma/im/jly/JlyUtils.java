package org.enigma.im.jly;

import androidx.room.ColumnInfo;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import io.reactivex.Flowable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * author:  hedongjin
 * date:  2019-06-04
 * description: Please contact me if you have any questions
 */
public class JlyUtils {

    /***
     * 获取实现的Dao接口的列表
     * @param jlyDaoElement
     * @param roomDaoSet
     * @return
     */
    public static List<TypeElement> getRoomDaoInterfaces(TypeElement jlyDaoElement, Set<TypeElement> roomDaoSet) {
        return roomDaoSet.stream().filter(
                element -> jlyDaoElement.getInterfaces().stream().filter((Predicate<TypeMirror>) mirror -> equals(mirror, element)).count() >= 1
        ).collect(Collectors.toList());
    }

    /***
     * 判断集合是否为空
     * @param collection
     * @return
     */
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isAbstractMethod(Element element) {
        if (element instanceof ExecutableElement) {
            for (Modifier modifier : element.getModifiers()) {
                if (modifier == Modifier.ABSTRACT) {
                    return true;
                }
            }
        }

        return false;
    }

    /***
     * build一个方法头
     * @param element
     * @return
     */
    public static MethodSpec.Builder buildMethodHead(ExecutableElement element) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(element.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(element.getReturnType()));

        forEach(element.getParameters(), (index, size, paramElement) ->

                builder.addParameter(
                        ParameterSpec.builder(
                                ClassName.get(paramElement.asType()),
                                paramElement.getSimpleName().toString()
                        ).addAnnotations(
                                buildParameterAnnotation(paramElement)
                        ).build()
                ));

        return builder;
    }

    private static List<AnnotationSpec> buildParameterAnnotation(VariableElement paramElement) {
        List<AnnotationSpec> annotationList = new ArrayList<>();
        for (AnnotationMirror mirror : paramElement.getAnnotationMirrors()) {
            annotationList.add(AnnotationSpec.get(mirror));
        }
        return annotationList;
    }

    /***
     * 是否为相同类型
     * @param mirror
     * @param element
     * @return
     */
    private static boolean equals(TypeMirror mirror, TypeElement element) {
        return element.getQualifiedName().toString().equals(ClassName.get(mirror).toString());
    }

    public static boolean equals(TypeMirror mirror, ClassName cls) {
        return cls.toString().equals(ClassName.get(mirror).toString());
    }

    public static boolean equals(TypeMirror mirror, String canonicalName) {
        return canonicalName.equals(ClassName.get(mirror).toString());
    }

    public static String getColumnName(VariableElement element) {
        String columnName = element.getSimpleName().toString();
        ColumnInfo columnInfo = element.getAnnotation(ColumnInfo.class);
        if (columnInfo != null) {
            columnName = columnInfo.name();
        }

        return columnName;
    }

    public static void parseBoolean(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getInt($N)", Integer.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N != 0)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = ($N != 0)", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseInt(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getInt($N)", Integer.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseInt(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toOuterElement.getSimpleName().toString();

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getInt($N)", Integer.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N.$N($N))", entityName, methodName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N.$N($N)", entityName, fieldName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseLong(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getLong($N)", Long.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseFloat(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getFloat($N)", Float.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseDouble(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getDouble($N)", Double.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseDouble(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toOuterElement.getSimpleName().toString();

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getDouble($N)", Double.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N.$N($N)", entityName, methodName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N.$N($N)", entityName, fieldName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseString(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getString($N)", String.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseString(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toOuterElement.getSimpleName().toString();

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getString($N)", String.class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N.$N($N))", entityName, methodName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N.$N($N)", entityName, fieldName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseBlob(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getBlob($N)", byte[].class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    public static void parseBlob(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String columnName = JlyUtils.getColumnName((VariableElement) element);
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toOuterElement.getSimpleName().toString();

        builder.beginControlFlow("if ($N != -1) ", columnName);
        builder.addStatement("$T $N = $N.getBlob($N)", byte[].class, JlyConstant.JLY_VALUE_NAME, JlyConstant.JLY_CURSOR_NAME, columnName);
        builder.beginControlFlow("if ($N != null) ", JlyConstant.JLY_VALUE_NAME);

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N.$N($N))", entityName, methodName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        } else {
            builder.addStatement("$N.$N = $N.$N($N)", entityName, fieldName, converterName, converterMethod, JlyConstant.JLY_VALUE_NAME);
        }

        builder.endControlFlow();
        builder.endControlFlow();
    }

    /***
     * 属性赋值赋值
     * @param builder
     * @param entityName
     * @param fieldName
     * @param valueName
     * @param element
     */
    public static void fieldAssignment(MethodSpec.Builder builder, String entityName, String fieldName, String valueName, Element element) {

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getSetMethodName(fieldName);
            builder.addStatement("$N.$N($N)", entityName, methodName, valueName);
        } else {
            builder.addStatement("$N.$N = $N", entityName, fieldName, valueName);
        }

    }

    public static void bindBoolean(MethodSpec.Builder builder, String params) {
        builder.addStatement("$N.bindLong($N++, $N ? 1 : 0)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, params);
    }

    public static void bindBoolean(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);
            builder.addStatement("$N.bindLong($N++, $N.$N() ? 1 : 0)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, methodName);
        } else {
            builder.addStatement("$N.bindLong($N++, $N.$N ? 1 : 0)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, fieldName);
        }

    }

    public static void bindLong(MethodSpec.Builder builder, String params) {
        builder.addStatement("$N.bindLong($N++, $N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, params);
    }

    public static void bindLong(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        if (!element.asType().getKind().isPrimitive()) {
            if (!isPublicField(element)) {
                String methodName = JlyUtils.getGetMethodName(fieldName);
                builder.beginControlFlow("if($N.$N() == null)", entityName, methodName);
                builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
                builder.nextControlFlow("else");
                builder.addStatement("$N.bindLong($N++, $N.$N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, methodName);
                builder.endControlFlow();
            } else {
                builder.beginControlFlow("if($N.$N == null)",entityName, fieldName);
                builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
                builder.nextControlFlow("else");
                builder.addStatement("$N.bindLong($N++, $N.$N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, fieldName);
                builder.endControlFlow();
            }
        } else  {
            if (!isPublicField(element)) {
                String methodName = JlyUtils.getGetMethodName(fieldName);
                builder.addStatement("$N.bindLong($N++, $N.$N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, methodName);
            } else {
                builder.addStatement("$N.bindLong($N++, $N.$N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, fieldName);
            }
        }
    }

    public static void bindDouble(MethodSpec.Builder builder, String params) {
        builder.addStatement("$N.bindDouble($N++, $N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, params);
    }

    public static void bindDouble(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);
            builder.addStatement("$N.bindDouble($N++, $N.$N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, methodName);
        } else {
            builder.addStatement("$N.bindDouble($N++, $N.$N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, fieldName);
        }
    }

    public static void bindString(MethodSpec.Builder builder, String params) {
        builder.beginControlFlow("if($N == null)", params);
        builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
        builder.nextControlFlow("else");
        builder.addStatement("$N.bindString($N++, $N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, params);
        builder.endControlFlow();
    }

    public static void bindString(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);

            builder.beginControlFlow("if($N.$N() == null)", entityName, methodName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindString($N++, $N.$N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, methodName);
            builder.endControlFlow();

        } else {
            builder.beginControlFlow("if($N.$N == null)", entityName, fieldName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindString($N++, $N.$N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, fieldName);
            builder.endControlFlow();
        }
    }

    public static void bindBlob(MethodSpec.Builder builder, String params) {
        builder.beginControlFlow("if($N == null)", params);
        builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
        builder.nextControlFlow("else");
        builder.addStatement("$N.bindBlob($N++, $N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, params);
        builder.endControlFlow();
    }

    public static void bindBlob(MethodSpec.Builder builder, String entityName, Element element) {
        String fieldName = element.getSimpleName().toString();
        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);

            builder.beginControlFlow("if($N.$N() == null)", entityName, methodName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindBlob($N++, $N.$N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, methodName);
            builder.endControlFlow();

        } else {
            builder.beginControlFlow("if($N.$N == null)", entityName, fieldName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindBlob($N++, $N.$N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, entityName, fieldName);
            builder.endControlFlow();
        }
    }

    public static void bindLong(MethodSpec.Builder builder, String params, JlyConverterElement converterElement) {
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();
        builder.addStatement("$N.bindLong($N++, $N.$N($N))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, params);
    }

    public static void bindLong(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);
            builder.addStatement("$N.bindLong($N++, $N.$N($N.N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, methodName);
        } else {
            builder.addStatement("$N.bindLong($N++, $N.$N($N.$N))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, fieldName);
        }

    }

    public static void bindDouble(MethodSpec.Builder builder, String params, JlyConverterElement converterElement) {
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();
        builder.addStatement("$N.bindDouble($N++, $N.$N($N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, params);
    }

    public static void bindDouble(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);
            builder.addStatement("$N.bindDouble($N++, $N.$N($N.$N())", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, methodName);
        } else {
            builder.addStatement("$N.bindDouble($N++, $N.$N($N.$N))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, fieldName);
        }

    }


    public static void bindString(MethodSpec.Builder builder, String params, JlyConverterElement converterElement) {
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();

        builder.beginControlFlow("if($N == null)", params);
        builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
        builder.nextControlFlow("else");
        builder.addStatement("$N.bindString($N++, $N.$N($N))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, params);
        builder.endControlFlow();
    }

    public static void bindString(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);
            builder.beginControlFlow("if($N.$N() == null)", entityName, methodName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindString($N++, $N.$N($N.$N()))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, methodName);
            builder.endControlFlow();
        } else {
            builder.beginControlFlow("if($N.$N == null)", entityName, fieldName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindString($N++, $N.$N($N.$N))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, fieldName);
            builder.endControlFlow();
        }

    }


    public static void bindBlob(MethodSpec.Builder builder, String params, JlyConverterElement converterElement) {
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();

        builder.beginControlFlow("if($N == null)", params);
        builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
        builder.nextControlFlow("else");
        builder.addStatement("$N.bindBlob($N++, $N.$N($N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, params);
        builder.endControlFlow();
    }

    public static void bindBlob(MethodSpec.Builder builder, String entityName, Element element, JlyConverterElement converterElement) {
        String fieldName = element.getSimpleName().toString();
        String converterName = toLower(converterElement.typeElement.getSimpleName().toString());
        String converterMethod = converterElement.toDbElement.getSimpleName().toString();

        if (!isPublicField(element)) {
            String methodName = JlyUtils.getGetMethodName(fieldName);
            builder.beginControlFlow("if($N.$N() == null)", entityName, methodName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindBlob($N++, $N.$N($N.$N()))", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, methodName);
            builder.endControlFlow();
        } else {
            builder.beginControlFlow("if($N.$N == null)", entityName, fieldName);
            builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            builder.nextControlFlow("else");
            builder.addStatement("$N.bindBlob($N++, $N.$N($N.$N)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME, converterName, converterMethod, entityName, fieldName);
            builder.endControlFlow();
        }

    }

    public static void bindArray(MethodSpec.Builder builder, Element params) {
        String name = params.getSimpleName().toString();
        String value = "value";

        if (isType(params.asType(), Boolean[].class, boolean[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", Boolean.class, value, name);
            bindBoolean(builder, value);
            builder.endControlFlow();
        }

        if (isType(params.asType(), Short[].class, short[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", Short.class, value, name);
            bindLong(builder, value);
            builder.endControlFlow();
        }

        if (isType(params.asType(), Integer[].class, int[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", Integer.class, value, name);
            bindLong(builder, value);
            builder.endControlFlow();
        }

        if (isType(params.asType(), Long[].class, long[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", Long.class, value, name);
            bindLong(builder, value);
            builder.endControlFlow();
        }

        if (isType(params.asType(), Float[].class, float[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", Float.class, value, name);
            bindDouble(builder, value);
            builder.endControlFlow();
        }

        if (isType(params.asType(), Double[].class, double[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", Double.class, value, name);
            bindDouble(builder, value);
            builder.endControlFlow();
        }

        if (isType(params.asType(), String[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", String.class, value, name);
            bindDouble(builder, value);
            builder.endControlFlow();
        }
    }

    public static void bindList(MethodSpec.Builder builder, Element params) {
        String name = params.getSimpleName().toString();
        String value = "value";

        if (isListType(params.asType(), Byte.class)) {
            builder.beginControlFlow("for ($T $N: $N)", Byte.class, value, name);
            bindLong(builder, value);
            builder.endControlFlow();
        }

        if (isListType(params.asType(), Integer.class)) {
            builder.beginControlFlow("for ($T $N: $N)", Integer.class, value, name);
            bindLong(builder, value);
            builder.endControlFlow();
        }

        if (isListType(params.asType(), Long.class)) {
            builder.beginControlFlow("for ($T $N: $N)", Long.class, value, name);
            bindLong(builder, value);
            builder.endControlFlow();
        }

        if (isListType(params.asType(), Boolean.class)) {
            builder.beginControlFlow("for ($T $N: $N)", Boolean.class, value, name);
            bindBoolean(builder, value);
            builder.endControlFlow();
        }

        if (isListType(params.asType(), String.class)) {
            builder.beginControlFlow("for ($T $N: $N)", String.class, value, name);
            bindString(builder, value);
            builder.endControlFlow();
        }

        if (isListType(params.asType(), byte[].class)) {
            builder.beginControlFlow("for ($T $N: $N)", byte[].class, value, name);
            bindBlob(builder, value);
            builder.endControlFlow();
        }
    }

    public static boolean isNotStaticField(Element element) {
        if (!(element instanceof VariableElement)) {
            return false;
        }

        for (Modifier modifier : element.getModifiers()) {
            if (modifier == Modifier.STATIC) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPublicField(Element element) {
        if (!(element instanceof VariableElement)) {
            return false;
        }

        for (Modifier modifier : element.getModifiers()) {
            if (modifier == Modifier.PUBLIC) {
                return true;
            }
        }

        return false;
    }


    /***
     * 判断是否为ByteArray
     * @param element
     * @return
     */
    public static boolean isByteArray(Element element) {
        return isByteArray(element.asType());
    }

    /***
     * 判断是否为ByteArray
     * @param typeMirror
     * @return
     */
    public static boolean isByteArray(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.ARRAY) {
            return false;
        }

        return isType(typeMirror, Byte[].class, byte[].class);
    }

    /***
     * 判断是否为基础类型Array
     * @param element
     * @return
     */
    public static boolean isPrimitiveArray(Element element) {
        return isPrimitiveArray(element.asType());
    }

    /***
     * 判断是否为基础类型Array
     * @param typeMirror
     * @return
     */
    public static boolean isPrimitiveArray(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.ARRAY) {
            return false;
        }

        if (isType(typeMirror, Boolean[].class, boolean[].class)) {
            return true;
        }

        if (isType(typeMirror, Byte[].class, byte[].class)) {
            return true;
        }

        if (isType(typeMirror, Short[].class, short[].class)) {
            return true;
        }

        if (isType(typeMirror, Integer[].class, int[].class)) {
            return true;
        }

        if (isType(typeMirror, Long[].class, long[].class)) {
            return true;
        }

        if (isType(typeMirror, Float[].class, float[].class)) {
            return true;
        }

        if (isType(typeMirror, Double[].class, double[].class)) {
            return true;
        }

        if (isType(typeMirror, String[].class, String[].class)) {
            return true;
        }

        return false;
    }

    public static String getClass(TypeMirror typeMirror) {
        if (isType(typeMirror, boolean.class)) {
            return "boolean.class";
        }

        if (isType(typeMirror, byte.class)) {
            return "byte.class";
        }

        if (isType(typeMirror, short.class)) {
            return "short.class";
        }

        if (isType(typeMirror, int.class)) {
            return "int.class";
        }

        if (isType(typeMirror, long.class)) {
            return "long.class";
        }

        if (isType(typeMirror, float.class)) {
            return "float.class";
        }

        if (isType(typeMirror, double.class)) {
            return "double.class";
        }

        if (isList(typeMirror)) {
            return "List.class";
        }

        return null;
    }

    /***
     * 判断是否为基础类型Array
     * @param typeMirror
     * @return
     */
    public static boolean isPrimitive(TypeMirror typeMirror) {
        if (isType(typeMirror, Boolean.class, boolean.class)) {
            return true;
        }

        if (isType(typeMirror, Byte.class, byte.class)) {
            return true;
        }

        if (isType(typeMirror, Short.class, short.class)) {
            return true;
        }

        if (isType(typeMirror, Integer.class, int.class)) {
            return true;
        }

        if (isType(typeMirror, Long.class, long.class)) {
            return true;
        }

        if (isType(typeMirror, Float.class, float.class)) {
            return true;
        }

        if (isType(typeMirror, Double.class, double.class)) {
            return true;
        }

        if (isType(typeMirror, String.class)) {
            return true;
        }

        return false;
    }

    public static boolean isType(TypeMirror typeMirror, Class... clsArray) {
        for (Class cls : clsArray) {

            if (cls.getCanonicalName().equals(typeMirror.toString())) {
                return true;
            }

            if (cls.getSimpleName().equals(typeMirror.toString())) {
                return true;
            }

            try {
                if (ClassName.get(typeMirror).equals(ClassName.get(cls))) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    /***
     * 判断是否为array
     * @param element
     * @return
     */
    public static boolean isArray(Element element) {
        return ClassName.get(element.asType()).toString().endsWith("[]");
    }

    /***
     * 获取list的泛型类型
     * @return
     */
    public static String getArrayType(TypeMirror typeMirror) {
        String fullName = typeMirror.toString();
        return fullName.substring(0, fullName.indexOf("["));
    }


    /***
     * 判断是否为list
     * @param element
     * @return
     */
    public static boolean isList(Element element) {
        return isList(element.asType());
    }

    public static boolean isList(TypeMirror typeMirror) {
        return ClassName.get(typeMirror).toString().startsWith(ClassName.get(List.class).toString());
    }

    public static boolean isFlowList(TypeMirror typeMirror) {
        return typeMirror.toString().startsWith(Flowable.class.getCanonicalName() + "<" + List.class.getCanonicalName());
    }

    public static String getFlowType(TypeMirror typeMirror) {
        String fullName = ClassName.get(typeMirror).toString();
        int startIndex = (Flowable.class.getCanonicalName() + "<").length();
        return fullName.substring(startIndex, fullName.length() - 1);
    }

    public static String getFlowListType(TypeMirror typeMirror) {
        String flowType = getFlowType(typeMirror);
        int startIndex = (List.class.getCanonicalName() + "<").length();
        return flowType.substring(startIndex, flowType.length() - 1);
    }

    /***
     * 获取list的泛型类型
     * @param typeMirror
     * @return
     */
    public static String getListType(TypeMirror typeMirror) {
        String fullName = ClassName.get(typeMirror).toString();
        int startIndex = (List.class.getCanonicalName() + "<").length();
        return fullName.substring(startIndex, fullName.length() - 1);
    }

    /***
     * 判断泛型是否为指定的类型
     * @param typeMirror
     * @param cls
     * @return
     */
    public static boolean isListType(TypeMirror typeMirror, Class cls) {
        return getListType(typeMirror).equals(cls.getCanonicalName());
    }


    public static boolean isString(Element params) {
        return isString(params.asType());
    }

    public static boolean isInt(TypeMirror mirror) {
        return ClassName.get(mirror).equals(ClassName.get(Integer.class));
    }

    public static boolean isDouble(TypeMirror mirror) {
        return ClassName.get(mirror).equals(ClassName.get(Double.class));
    }

    public static boolean isString(TypeMirror mirror) {
        return ClassName.get(mirror).equals(ClassName.get(String.class));
    }

    public static boolean containJlyActionTable(Element element) {
        if (element.getAnnotationMirrors() == null) {
            return false;
        }

        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.toString().contains("JlyActionTable")) {
                return true;
            }
        }

        return false;
    }

    public static boolean containJlyObserveTable(Element element) {
        if (element.getAnnotationMirrors() == null) {
            return false;
        }

        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.toString().contains("JlyObserveTable")) {
                return true;
            }
        }

        return false;
    }

    public static JlyConverterElement getConverterElement(Element element, Set<TypeElement> converterSet) {

        ColumnInfo columnInfo = element.getAnnotation(ColumnInfo.class);
        if (columnInfo == null) {
            return null;
        }

        int dbType = columnInfo.typeAffinity();
        if (dbType == ColumnInfo.UNDEFINED) {
            return null;
        }


        TypeMirror outerType = element.asType();
        for (TypeElement typeElement : converterSet) {

            ExecutableElement toDbElement = null;
            ExecutableElement toOuterElement = null;

            for (Element childElement : typeElement.getEnclosedElements()) {
                if (isAbstractMethod(childElement)) {
                    continue;
                }

                if (JlyConstant.JLY_INIT_NAME.equals(childElement.getSimpleName().toString())) {
                    continue;
                }

                ExecutableElement executableElement = (ExecutableElement) childElement;
                // outer -> db
                if (JlyUtils.isConvertMethod(executableElement, outerType, dbType)) {
                    toDbElement = executableElement;
                }

                // db -> outer
                if (JlyUtils.isConvertMethod(executableElement, dbType, outerType)) {
                    toOuterElement = executableElement;
                }

            }

            if (toDbElement != null && toOuterElement != null) {
                return new JlyConverterElement(dbType, typeElement, toDbElement, toOuterElement);
            }
        }

        return null;

    }

    public static boolean isConvertMethod(ExecutableElement element, TypeMirror from, int to) {
        VariableElement variableElement = element.getParameters().get(0);
        if (!variableElement.asType().toString().equals(from.toString())) {
            return false;
        }

        switch (to) {
            case ColumnInfo.INTEGER:
                if (!isInt(element.getReturnType())) return false;
                break;
            case ColumnInfo.REAL:
                if (!isDouble(element.getReturnType())) return false;
                break;
            case ColumnInfo.TEXT:
                if (!isString(element.getReturnType())) return false;
                break;
            case ColumnInfo.BLOB:
                if (!isByteArray(element.getReturnType())) return false;
                break;
        }

        return true;


    }

    public static boolean isConvertMethod(ExecutableElement element, int from, TypeMirror to) {

        VariableElement variableElement = element.getParameters().get(0);
        switch (from) {
            case ColumnInfo.INTEGER:
                if (!isInt(variableElement.asType())) return false;
                break;
            case ColumnInfo.REAL:
                if (!isDouble(variableElement.asType())) return false;
                break;
            case ColumnInfo.TEXT:
                if (!isString(variableElement.asType())) return false;
                break;
            case ColumnInfo.BLOB:
                if (!isByteArray(variableElement.asType())) return false;
                break;
        }

        if (!element.getReturnType().toString().equals(to.toString())) {
            return false;
        }

        return true;


    }

    /***
     * 获取方法参数
     * @param element
     * @return
     */
    public static String getMethodParams(ExecutableElement element) {
        final StringBuffer paramsBuffer = new StringBuffer();
        forEach(element.getParameters(), (index, size, paramElement) -> {
            paramsBuffer.append(paramElement.getSimpleName().toString());
            if (index < size - 1) {
                paramsBuffer.append(", ");
            }
        });
        return paramsBuffer.toString();
    }

    public static String getSetMethodName(String fieldName) {
        return "set" + toUpper(fieldName);
    }

    public static String getGetMethodName(String fieldName) {
        return "get" + toUpper(fieldName);
    }

    /**
     * 获取参数长度
     *
     * @param element
     * @return
     */
    public static int getMethodParamsLength(ExecutableElement element) {
        return element.getParameters() == null ? 0 : element.getParameters().size();
    }

    public static ClassName get(String canonicalName) {
        return ClassName.get(getPackageName(canonicalName), getClassName(canonicalName));
    }

    public static String getClassName(Element element) {
        if (element instanceof TypeElement) {
            return getClassName((TypeElement) element);
        }
        if (element instanceof ExecutableElement) {
            return getClassName((ExecutableElement) element);
        }
        if (element instanceof VariableElement) {
            return getClassName((VariableElement) element);
        }
        return null;
    }

    private static String getClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString();
    }

    private static String getClassName(VariableElement element) {
        return getClassName((TypeElement) element.getEnclosingElement());
    }

    private static String getClassName(ExecutableElement element) {
        return getClassName((TypeElement) element.getEnclosingElement());
    }

    public static String getPackageName(Elements utils, Element element) {
        if (element instanceof TypeElement) {
            return getPackageName(utils, (TypeElement) element);
        }
        if (element instanceof ExecutableElement) {
            return getPackageName(utils, (ExecutableElement) element);
        }
        if (element instanceof VariableElement) {
            return getPackageName(utils, (VariableElement) element);
        }
        return null;
    }


    private static String getPackageName(Elements utils, VariableElement element) {
        return utils.getPackageOf(element.getEnclosingElement()).getQualifiedName().toString();
    }

    private static String getPackageName(Elements utils, ExecutableElement element) {
        return utils.getPackageOf(element.getEnclosingElement()).getQualifiedName().toString();
    }

    private static String getPackageName(Elements utils, TypeElement element) {
        return utils.getPackageOf(element).getQualifiedName().toString();
    }

    /**
     * @param database
     * @return
     * @com.enigma.im.jly.JlyDatabase(value=com.example.gavin.apttest.AppDatabase)
     */
    public static String getCanonicalName(JlyDatabase database) {
        String databaseStr = database.toString();
        int startIndex = ("@" + JlyDatabase.class.getCanonicalName() + "(value=").length();
        return databaseStr.substring(startIndex, databaseStr.length() - 1);
    }

    public static String getPackageName(JlyDatabase database) {
        String canonicalName = getCanonicalName(database);
        return canonicalName.substring(0, canonicalName.lastIndexOf("."));
    }

    public static String getClassName(JlyDatabase database) {
        String canonicalName = getCanonicalName(database);
        return canonicalName.substring(canonicalName.lastIndexOf(".") + 1);
    }

    public static String getPackageName(String canonicalName) {
        return canonicalName.substring(0, canonicalName.lastIndexOf("."));
    }

    public static String getClassName(String canonicalName) {
        return canonicalName.substring(canonicalName.lastIndexOf(".") + 1);
    }

    public static String toLower(String canonicalName) {
        String simpleName = getClassName(canonicalName);
        if (Character.isLowerCase(simpleName.charAt(0))) {
            return simpleName;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(simpleName.charAt(0))).append(simpleName.substring(1)).toString();
        }
    }

    public static String toUpper(String canonicalName) {
        if (Character.isUpperCase(canonicalName.charAt(0))) {
            return canonicalName;
        } else {
            return (new StringBuilder()).append(Character.toUpperCase(canonicalName.charAt(0))).append(canonicalName.substring(1)).toString();
        }
    }

    public static ClassName forJlyDatabase(JlyDatabase database) {
        return ClassName.get(getPackageName(database), getClassName(database));
    }

    public static <T> void forEach(List<T> list, IFunc<T> func) {
        if (list == null || list.isEmpty()) {
            return;
        }

        int size = list.size();
        for (int index = 0; index < size; index++) {
            func.apply(index, size, list.get(index));
        }
    }

    public static <T> void forEach(Set<T> list, IFunc<T> func) {
        if (list == null || list.isEmpty()) {
            return;
        }

        int index = 0;
        int size = list.size();
        for (T t : list) {
            func.apply(index, size, t);
            index++;
        }
    }

    interface IFunc<T> {
        void apply(int index, int size, T t);
    }
}
