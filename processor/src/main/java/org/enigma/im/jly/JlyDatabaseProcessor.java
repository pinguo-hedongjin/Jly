package org.enigma.im.jly;

import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JlyDatabaseProcessor extends BaseProcessor<Void> {

    private Set<TypeElement> jlyDbSet;
    private Set<TypeElement> jlyDaoSet;
    private Set<TypeElement> roomDaoSet;


    public JlyDatabaseProcessor(Elements utils, Filer filer, Printer printer, Set<TypeElement> jlyDbSet, Set<TypeElement> jlyDaoSet, Set<TypeElement> roomDaoSet) {
        super(utils, filer, printer);
        this.jlyDbSet = jlyDbSet;
        this.jlyDaoSet = jlyDaoSet;
        this.roomDaoSet = roomDaoSet;
    }

    @Override
    public Void process() {
        try {
            printer.i(TAG, "process start...");
            processDatabase();
        } finally {
            printer.i(TAG, "process end...");
        }
        return null;
    }

    private void processDatabase() {
        if (jlyDbSet == null || jlyDbSet.isEmpty()) {
            printer.i(TAG, " 没有解析到JlyDatabase注解");
        }

        for (TypeElement jlyDbElement : jlyDbSet) {
            processDatabase(jlyDbElement);
        }
    }

    private void processDatabase(TypeElement jlyDbElement) {

        JlyDatabase jlyDb = jlyDbElement.getAnnotation(JlyDatabase.class);

        // 构建类
        TypeSpec.Builder typeBuilder = buildTypeBuilder(jlyDbElement);

        // 构建属性
        typeBuilder.addField(
                FieldSpec.builder(
                        JlyUtils.forJlyDatabase(jlyDb),
                        JlyConstant.JLY_DB_NAME
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL).build()
        );
        typeBuilder.addFields(
                buildFieldList(jlyDbElement)
        );


        // 构建构造方法
        typeBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                ParameterSpec.builder(
                                        JlyUtils.forJlyDatabase(jlyDb),
                                        JlyConstant.JLY_DB_NAME
                                ).build()
                        )
                        .addStatement("super($N)", JlyConstant.JLY_DB_NAME)
                        .addStatement("this.$N = $N", JlyConstant.JLY_DB_NAME, JlyConstant.JLY_DB_NAME)
                        .build()
        );

        // 构建方法
        typeBuilder.addMethods(buildMethodList(jlyDbElement));

        // 构建java文件
        try {
            buildJavaFile(jlyDbElement, typeBuilder.build()).writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeSpec.Builder buildTypeBuilder(TypeElement daoElement) {
        return TypeSpec.classBuilder(JlyUtils.getClassName(daoElement) + JlyConstant.JLY_DAO_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(JlyUtils.getPackageName(utils, daoElement), JlyUtils.getClassName(daoElement)));
    }

    private List<FieldSpec> buildFieldList(TypeElement jlyDaoElement) {
        List<FieldSpec> result = new ArrayList<>();

        for (Element element : jlyDaoElement.getEnclosedElements()) {

            if (JlyUtils.isAbstractMethod(element)) {
                result.add(buildFieldSpec((ExecutableElement) element));
            }


        }

        return result;
    }

    private FieldSpec buildFieldSpec(ExecutableElement element) {
        String canonicalName = element.getReturnType().toString();
        ClassName jlyDao = ClassName.get(JlyUtils.getPackageName(canonicalName), JlyUtils.getClassName(canonicalName) + JlyConstant.JLY_DAO_SUFFIX);

        return FieldSpec.builder(
                jlyDao, JlyUtils.toLower(canonicalName), Modifier.PRIVATE, Modifier.VOLATILE
        ).build();
    }

    private List<MethodSpec> buildMethodList(TypeElement jlyDaoElement) {

        List<MethodSpec> result = new ArrayList<>();

        for (Element element : jlyDaoElement.getEnclosedElements()) {

            if (JlyUtils.isAbstractMethod(element)) {
                result.add(buildMethodSpec((ExecutableElement) element));
            }
        }

        return result;
    }


    private MethodSpec buildMethodSpec(ExecutableElement element) {
        String params = JlyUtils.getMethodParams(element);
        String canonicalName = element.getReturnType().toString();
        String fieldName = JlyUtils.toLower(canonicalName);
        ClassName jlyDao = ClassName.get(JlyUtils.getPackageName(canonicalName), JlyUtils.getClassName(canonicalName) + JlyConstant.JLY_DAO_SUFFIX);

        MethodSpec.Builder builder = JlyUtils.buildMethodHead(element);
        builder.beginControlFlow("if ($N != null)", fieldName);
        builder.addStatement("return $N", fieldName);
        builder.nextControlFlow("else");
        builder.beginControlFlow("synchronized(this)");
        builder.beginControlFlow("if ($N == null)", fieldName);

        if (hasRoomDao(canonicalName)) {
            builder.addStatement("$N = new $T($N, $N.$N($N))", fieldName, jlyDao, JlyConstant.JLY_DB_NAME, JlyConstant.JLY_DB_NAME, element.getSimpleName().toString(), params);
        } else {
            builder.addStatement("$N = new $T($N)", fieldName, jlyDao, JlyConstant.JLY_DB_NAME);
        }

        builder.endControlFlow();
        builder.addStatement("return $N", fieldName);
        builder.endControlFlow();
        builder.endControlFlow();

        return builder.build();
    }

    private JavaFile buildJavaFile(TypeElement daoElement, TypeSpec typeSpec) {
        return JavaFile.builder(JlyUtils.getPackageName(utils, daoElement), typeSpec).build();
    }

    private boolean hasRoomDao(String canonicalName) {
        for (TypeElement element : jlyDaoSet) {
            if (canonicalName.equals(element.getQualifiedName().toString())
                    && JlyUtils.isEmpty(JlyUtils.getRoomDaoInterfaces(element, roomDaoSet))) {
                return false;
            }
        }

        return true;
    }


}


