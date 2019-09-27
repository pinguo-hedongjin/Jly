package org.enigma.im.jly;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Ignore;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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
public class JlyEntityInsertProcessor extends BaseProcessor<Void> {

    private Set<TypeElement> entitySet;
    private Set<TypeElement> converterSet;

    public JlyEntityInsertProcessor(
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
            processInsert();
        } finally {
            printer.i(TAG, "process end...");
        }
        return null;
    }

    private void processInsert() {
        for (TypeElement entityElement : entitySet) {
            processInsert(entityElement);
        }
    }

    private void processInsert(TypeElement entityElement) {
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

        typeBuilder.addField(
                FieldSpec.builder(
                        String.class,
                        JlyConstant.JLY_TABLE_NAME
                )
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build()
        );

        // 构建构造方法
        typeBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                ParameterSpec.builder(
                                        JlyConstant.JLY_DB_TYPE,
                                        JlyConstant.JLY_DB_NAME
                                ).build()
                        )
                        .addParameter(
                                ParameterSpec.builder(
                                        String.class,
                                        JlyConstant.JLY_TABLE_NAME
                                ).build()
                        )
                        .addStatement("super($N)", JlyConstant.JLY_DB_NAME)
                        .addStatement("this.$N = $N", JlyConstant.JLY_TABLE_NAME, JlyConstant.JLY_TABLE_NAME)
                        .build()
        );

        // 构建方法
        typeBuilder.addMethod(buildQueryMethod(entityElement));
        typeBuilder.addMethod(buildBindMethod(entityElement));

        // 构建java文件
        try {
            buildJavaFile(entityElement, typeBuilder.build()).writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeSpec.Builder buildTypeBuilder(TypeElement daoElement) {
        return TypeSpec.classBuilder(JlyUtils.getClassName(daoElement) + JlyConstant.JLY_INSERT_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(JlyConstant.JLY_INSERT_TYPE, ClassName.get(daoElement.asType())));
    }

    private JavaFile buildJavaFile(TypeElement daoElement, TypeSpec typeSpec) {
        return JavaFile.builder(JlyUtils.getPackageName(utils, daoElement), typeSpec).build();
    }

    private MethodSpec buildQueryMethod(TypeElement entityElement) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createQuery")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class);

        buildQuerySql(builder, entityElement);

        return builder.build();
    }

    private MethodSpec buildBindMethod(TypeElement entityElement) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("bind")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(JlyConstant.JLY_STMT_TYPE, JlyConstant.JLY_STMT_NAME)
                .addParameter(ClassName.get(entityElement), JlyConstant.JLY_ITEM_NAME)
                .addStatement("$T $N = 1", int.class, JlyConstant.JLY_OFFSET_NAME);


        bindNonNull(builder, JlyConstant.JLY_ITEM_NAME, entityElement);


        return builder.build();
    }

    /***
     *  构建插入的sql
     * @param entityElement
     */
    private void buildQuerySql(MethodSpec.Builder builder, TypeElement entityElement) {
        StringBuffer columnBuffer = new StringBuffer();
        StringBuffer stubBuffer = new StringBuffer();

        List<String> columnList = getColumnList(new ArrayList<>(), entityElement);
        int length = columnList.size();

        for (int index = 0; index < length; index++) {
            columnBuffer.append("`");
            columnBuffer.append(columnList.get(index));
            columnBuffer.append("`");

            stubBuffer.append("?");

            if (index < length -1) {
                columnBuffer.append(",");
                stubBuffer.append(",");
            }
        }

        builder.addStatement("return $S + $N + $S", "INSERT OR REPLACE INTO `", JlyConstant.JLY_TABLE_NAME,
                "`(" + columnBuffer.toString() + ") VALUES(" + stubBuffer.toString() + ")");
    }


    private List<String> getColumnList(List<String> list, TypeElement entityElement) {
        // 构造index列表
        for (Element fieldElement : entityElement.getEnclosedElements()) {
            if (!JlyUtils.isNotStaticField(fieldElement)) continue;
            if (isIgnoreField(fieldElement))continue;

            if (isPrimitive(fieldElement) || JlyUtils.getConverterElement(fieldElement, converterSet) != null) {
                list.add(JlyUtils.getColumnName((VariableElement) fieldElement));
            } else if (isEmbeddedField(fieldElement)) {
                TypeElement fieldEntityElement = findEntity(fieldElement.asType().toString());
                if (fieldEntityElement != null) {
                    getColumnList(list, fieldEntityElement);
                }
            }
        }

        return list;
    }

    private void bindNonNull(MethodSpec.Builder builder, String entityName, TypeElement entityElement) {

        for (Element fieldElement : entityElement.getEnclosedElements()) {
            if (!JlyUtils.isNotStaticField(fieldElement)) continue;
            if (isIgnoreField(fieldElement))continue;

            if (isPrimitive(fieldElement)) {
                bindField(builder, entityName, fieldElement);
            } else {
                JlyConverterElement element = JlyUtils.getConverterElement(fieldElement, converterSet);
                if (element != null) {
                    bindField(builder, entityName, fieldElement, element);
                } else if (isEmbeddedField(fieldElement)) {

                    String fieldName = fieldElement.getSimpleName().toString();
                    String itemName = "_" + fieldName;
                    if (!JlyUtils.isPublicField(fieldElement)) {
                        String methodName = JlyUtils.getGetMethodName(fieldName);
                        builder.addStatement("$T $N = $N.$N()", ClassName.get(fieldElement.asType()), itemName, entityName, methodName);
                    } else {
                        builder.addStatement("$T $N = $N", ClassName.get(fieldElement.asType()), itemName, fieldName);
                    }

                    TypeElement fieldEntityElement = findEntity(fieldElement.asType().toString());
                    if (fieldEntityElement != null) {

                        builder.beginControlFlow("if($N != null)", itemName);

                        bindNonNull(builder, itemName, fieldEntityElement);

                        builder.nextControlFlow("else");

                        bindNull(builder, itemName, fieldEntityElement);

                        builder.endControlFlow();
                    }
                }

            }
        }
    }

    private void bindNull(MethodSpec.Builder builder, String entityName, TypeElement entityElement) {
        for (Element fieldElement : entityElement.getEnclosedElements()) {
            if (!JlyUtils.isNotStaticField(fieldElement)) continue;
            if (isIgnoreField(fieldElement))continue;

            if (isPrimitive(fieldElement)) {
                builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
            } else {

                JlyConverterElement element = JlyUtils.getConverterElement(fieldElement, converterSet);
                if (element != null) {
                    builder.addStatement("$N.bindNull($N++)", JlyConstant.JLY_STMT_NAME, JlyConstant.JLY_OFFSET_NAME);
                } else if (isEmbeddedField(fieldElement)) {

                    TypeElement fieldEntityElement = findEntity(fieldElement.asType().toString());
                    if (fieldEntityElement != null) {
                        bindNull(builder, entityName, fieldEntityElement);
                    }
                }

            }
        }
    }

    /***
     * 解析基础属性
     * @param builder
     * @param params
     */
    private void bindField(MethodSpec.Builder builder, String entityName, Element params) {
        TypeMirror typeMirror = params.asType();
        if (JlyUtils.isType(typeMirror, Byte.class, byte.class, Short.class, short.class, Character.class, char.class, Integer.class, int.class, Long.class, long.class)) {
            JlyUtils.bindLong(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, Boolean.class, boolean.class)) {
            JlyUtils.bindBoolean(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, Float.class, float.class, Double.class, double.class)) {
            JlyUtils.bindDouble(builder, entityName, params);
        } else if (JlyUtils.isType(typeMirror, String.class)) {
            printer.i(TAG, "bindField = ");
            JlyUtils.bindString(builder, entityName, params);
        } else if (JlyUtils.isByteArray(params)) {
            JlyUtils.bindBlob(builder, entityName, params);
        } else if (JlyUtils.isPrimitiveArray(params)) {
            JlyUtils.bindArray(builder, params);
        } else if (JlyUtils.isList(params)) {
            JlyUtils.bindList(builder, params);
        }
    }

    /***
     * 解析Converter属性
     * @param builder
     * @param params
     * @param converterElement
     */
    protected void bindField(MethodSpec.Builder builder, String entityName, Element params, JlyConverterElement converterElement) {
        switch (converterElement.dbType) {
            case ColumnInfo.INTEGER:
                JlyUtils.bindLong(builder, entityName, params, converterElement);
                break;
            case ColumnInfo.REAL:
                JlyUtils.bindDouble(builder, entityName, params, converterElement);
                break;
            case ColumnInfo.TEXT:
                JlyUtils.bindString(builder, entityName, params, converterElement);
                break;
            case ColumnInfo.BLOB:
                JlyUtils.bindBlob(builder, entityName, params, converterElement);
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
     * 判断是否为Ignore
     * @param element
     * @return
     */
    public boolean isIgnoreField(Element element) {
        if (!(element instanceof VariableElement)) {
            return false;
        }

        return element.getAnnotation(Ignore.class) != null;
    }

    /***
     * 判断是否为Embedded
     * @param element
     * @return
     */
    public boolean isEmbeddedField(Element element) {
        if (!(element instanceof VariableElement)) {
            return false;
        }

        return element.getAnnotation(Embedded.class) != null;
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

}
