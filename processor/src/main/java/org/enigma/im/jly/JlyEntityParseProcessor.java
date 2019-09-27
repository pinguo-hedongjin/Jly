package org.enigma.im.jly;

import androidx.room.ColumnInfo;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * author:  hedongjin
 * date:  2019-08-02
 * description: Please contact me if you have any questions
 */
public class JlyEntityParseProcessor extends BaseProcessor<Void> {

    private Set<TypeElement> entitySet;
    private Set<TypeElement> converterSet;

    public JlyEntityParseProcessor(
            Elements utils,
            Filer filer,
            Printer printer,
            Set<TypeElement> entitySet,
            Set<TypeElement> converterSet) {
        super(utils, filer, printer);
        this.entitySet = entitySet;
        this.converterSet = converterSet;
    }

    @Override
    public Void process() {
        try {
            printer.i(TAG, "process start...");
            processParse();
        } finally {
            printer.i(TAG, "process end...");
        }
        return null;
    }

    private void processParse() {
        for (TypeElement entityElement : entitySet) {
            processParse(entityElement);
        }
    }

    private void processParse(TypeElement entityElement) {
        // 构建类
        TypeSpec.Builder typeBuilder = buildTypeBuilder(entityElement);

        // 构建属性
        for (TypeElement converter : converterSet) {
            typeBuilder.addField(
                    FieldSpec.builder(
                            ClassName.get(converter.asType()),
                            JlyUtils.toLower(converter.getSimpleName().toString())
                    )
                            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                            .initializer("new $T()", ClassName.get(converter.asType()))
                            .build()
            );
        }

        // 构建方法
        typeBuilder.addMethod(buildMethod(entityElement));

        // 构建java文件
        try {
            buildJavaFile(entityElement, typeBuilder.build()).writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeSpec.Builder buildTypeBuilder(TypeElement daoElement) {
        return TypeSpec.classBuilder(JlyUtils.getClassName(daoElement) + JlyConstant.JLY_PARSE_SUFFIX)
                .addModifiers(Modifier.PUBLIC);
    }

    private JavaFile buildJavaFile(TypeElement daoElement, TypeSpec typeSpec) {
        return JavaFile.builder(JlyUtils.getPackageName(utils, daoElement), typeSpec).build();
    }

    private MethodSpec buildMethod(TypeElement entityElement) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(JlyConstant.JLY_PARSE_METHOD)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterSpec.builder(
                            JlyConstant.JLY_CURSOR_TYPE, JlyConstant.JLY_CURSOR_NAME
                        ).build()
                )
                .returns(ClassName.get(entityElement.asType()));

        buildCursorIndex(builder, entityElement);

        fillCursorData(builder, JlyConstant.JLY_ITEM_NAME, entityElement);

        builder.addStatement("return $N", JlyConstant.JLY_ITEM_NAME);

        return builder.build();
    }

    /***
     * 构造CursorIndex
     * @param builder
     * @param entityElement
     */
    private void buildCursorIndex(MethodSpec.Builder builder, TypeElement entityElement) {
        // 构造index列表
        for (Element fieldElement : entityElement.getEnclosedElements()) {
            if (!JlyUtils.isNotStaticField(fieldElement)) continue;

            if (isPrimitive(fieldElement) || JlyUtils.getConverterElement(fieldElement, converterSet) != null) {
                String columnName = JlyUtils.getColumnName((VariableElement) fieldElement);
                builder.addStatement("final int $N = $N.getColumnIndex($S)", columnName, JlyConstant.JLY_CURSOR_NAME, columnName);
            } else {

                TypeElement fieldEntityElement = findEntity(fieldElement.asType().toString());
                if (fieldEntityElement != null) {
                    buildCursorIndex(builder, fieldEntityElement);
                }
            }
        }
    }

    /***
     * 填充cursor数据
     * @param builder
     * @param entityElement
     */
    private void fillCursorData(MethodSpec.Builder builder, String entityName, TypeElement entityElement) {

        builder.addStatement("final $T $N = new $T()", entityElement, entityName, entityElement);

        for (Element fieldElement : entityElement.getEnclosedElements()) {
            if (!JlyUtils.isNotStaticField(fieldElement)) continue;

            if (isPrimitive(fieldElement)) {
                parseField(builder, entityName, fieldElement);
            } else {

                JlyConverterElement element = JlyUtils.getConverterElement(fieldElement, converterSet);
                if (element != null) {
                    parseField(builder, entityName, fieldElement, element);
                } else {

                    TypeElement fieldEntityElement = findEntity(fieldElement.asType().toString());
                    if (fieldEntityElement != null) {

                        String conditions = getFillConditions(fieldEntityElement);
                        builder.beginControlFlow("if ($N)", conditions);

                        String fillName = JlyUtils.toLower(fieldEntityElement.getSimpleName().toString());
                        fillCursorData(builder, fillName, fieldEntityElement);

                        String fieldName = fieldElement.getSimpleName().toString();
                        JlyUtils.fieldAssignment(builder, entityName, fieldName, fillName, fieldElement);

                        builder.endControlFlow();

                    }
                }

            }
        }
    }

    /***
     * 解析基础属性
     * @param builder
     * @param entityName
     * @param params
     */
    private void parseField(MethodSpec.Builder builder, String entityName, Element params) {
        TypeMirror typeMirror = params.asType();
        if (JlyUtils.isType(typeMirror, Byte.class, byte.class, Short.class, short.class, Character.class, char.class, Integer.class, int.class)) {
            JlyUtils.parseInt(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, Long.class, long.class)) {
            JlyUtils.parseLong(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, Boolean.class, boolean.class)) {
            JlyUtils.parseBoolean(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, Float.class, float.class)) {
            JlyUtils.parseFloat(builder, entityName, params);
        } else if(JlyUtils.isType(typeMirror, Double.class, double.class)) {
            JlyUtils.parseDouble(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, String.class)) {
            JlyUtils.parseString(builder, entityName, params);
        } else if (TypeKind.ARRAY == typeMirror.getKind()) {
            JlyUtils.parseBlob(builder, entityName, params);
        }
    }

    /***
     * 解析Converter属性
     * @param builder
     * @param entityName
     * @param params
     * @param converterElement
     */
    protected void parseField(MethodSpec.Builder builder, String entityName, Element params, JlyConverterElement converterElement) {
        switch (converterElement.dbType) {
            case ColumnInfo.INTEGER:
                JlyUtils.parseInt(builder, entityName, params, converterElement);
                break;
            case ColumnInfo.REAL:
                JlyUtils.parseDouble(builder, entityName, params, converterElement);
                break;
            case ColumnInfo.TEXT:
                JlyUtils.parseString(builder, entityName, params, converterElement);
                break;
            case ColumnInfo.BLOB:
                JlyUtils.parseBlob(builder, entityName, params, converterElement);
                break;
        }
    }

    /***
     * 判断是否为基本数据类型
     * @return
     */
    private boolean isPrimitive(Element element) {
        return element.asType().getKind().isPrimitive() || JlyUtils.isInt(element.asType()) || JlyUtils.isByteArray(element) || JlyUtils.isString(element);
    }

    /***
     * 通过类名寻找对应实体
     * @param canonicalName
     * @return
     */
    private TypeElement findEntity(String canonicalName) {
        for (TypeElement element : entitySet) {
            if (element.getQualifiedName().toString().equals(canonicalName)) {
                return element;
            }
        }

        return null;
    }


    private String getFillConditions(TypeElement entityElement) {
        List<String> list = new ArrayList<>();
        fillConditions(list, entityElement);

        if (list.isEmpty()) {
            return "false";
        } else {
            StringBuffer conditions = new StringBuffer();

            int count = list.size();
            for (int i = 0; i < count; i++) {
                conditions.append("(");
                conditions.append(list.get(i)).append(" != ").append("-1");
                conditions.append(" && !");
                conditions.append(JlyConstant.JLY_CURSOR_NAME).append(".isNull(").append(list.get(i)).append("))");

                if (i < count - 1) {
                    conditions.append(" || ");
                }
            }

            return conditions.toString();
        }
    }

    private void fillConditions(List<String> list, TypeElement entityElement) {
        for (Element fieldElement : entityElement.getEnclosedElements()) {
            if (!JlyUtils.isNotStaticField(fieldElement)) continue;

            if (isPrimitive(fieldElement)) {
                list.add(JlyUtils.getColumnName((VariableElement) fieldElement));
            } else {
                TypeElement fieldEntityElement = findEntity(fieldElement.asType().toString());
                if (fieldEntityElement != null) {
                    fillConditions(list, fieldEntityElement);
                }
            }
        }
    }
}
