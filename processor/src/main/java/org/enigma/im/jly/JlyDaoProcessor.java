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

public class JlyDaoProcessor extends BaseProcessor<Void> {

    private Set<TypeElement> jlyDaoSet;
    private Set<TypeElement> jlyAopSet;
    private Set<TypeElement> jlyConverterSet;
    private Set<TypeElement> roomDaoSet;
    private Set<TypeElement> roomEntitySet;

    public JlyDaoProcessor(
            Elements utils,
            Filer filer,
            Printer printer,
            Set<TypeElement> jlyDaoSet,
            Set<TypeElement> jlyAopSet,
            Set<TypeElement> jlyConverterSet,
            Set<TypeElement> roomDaoSet,
            Set<TypeElement> roomEntitySet) {
        super(utils, filer, printer);
        this.jlyDaoSet = jlyDaoSet;
        this.jlyAopSet = jlyAopSet;
        this.jlyConverterSet = jlyConverterSet;
        this.roomDaoSet = roomDaoSet;
        this.roomEntitySet = roomEntitySet;
    }


    @Override
    public Void process() {
        try {
            printer.i(TAG, "process start...");
            processDao();
        } finally {
            printer.i(TAG, "process end...");
        }
        return null;
    }

    private void processDao() {
        // 1.生成标记JlyDao的实现类
        for (TypeElement jlyDaoElement : jlyDaoSet) {
            List<TypeElement> roomDaoElements = JlyUtils.getRoomDaoInterfaces(jlyDaoElement, roomDaoSet);
            if (roomDaoElements != null && roomDaoElements.size() > 1) {
                throw new RuntimeException("最多只能实现一个dao接口");
            }

            if (roomDaoElements == null || roomDaoElements.size() == 0) {
                processDao(jlyDaoElement);
            } else {
                processDao(jlyDaoElement, roomDaoElements.get(0));
            }
        }

        // 2.生成没有标记JlyDao的实现类
        for (TypeElement roomDaoElement : roomDaoSet) {
            processDao(roomDaoElement, roomDaoElement);
        }
    }

    private void processDao(TypeElement jlyDaoElement) {
        // 构建类
        TypeSpec.Builder typeBuilder = buildTypeBuilder(jlyDaoElement);

        // 构建属性
        typeBuilder.addField(
                FieldSpec.builder(
                        JlyConstant.JLY_DB_TYPE,
                        JlyConstant.JLY_DB_NAME
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL).build()
        );

        for (TypeElement converter : jlyConverterSet) {
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
                        .addStatement("this.$N = $N", JlyConstant.JLY_DB_NAME, JlyConstant.JLY_DB_NAME)
                .build()
        );

        // 构建方法
        typeBuilder.addMethods(buildMethodList(jlyDaoElement));

        // 构建java文件
        try {
            buildJavaFile(jlyDaoElement, typeBuilder.build()).writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processDao(TypeElement jlyDaoElement, TypeElement roomDaoElement) {
        // 构建类
        TypeSpec.Builder typeBuilder = buildTypeBuilder(jlyDaoElement);

        // 构建属性
        typeBuilder.addField(
                FieldSpec.builder(
                        JlyConstant.JLY_DB_TYPE,
                        JlyConstant.JLY_DB_NAME
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL).build()
        );

        typeBuilder.addField(
                FieldSpec.builder(
                        ClassName.get(JlyUtils.getPackageName(utils, roomDaoElement), JlyUtils.getClassName(roomDaoElement)),
                        JlyConstant.JLY_DAO_NAME
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL).build()
        );

        for (TypeElement converter : jlyConverterSet) {
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
                                        ClassName.get(JlyUtils.getPackageName(utils, roomDaoElement), JlyUtils.getClassName(roomDaoElement)),
                                        JlyConstant.JLY_DAO_NAME
                                ).build()
                        )
                        .addStatement("this.$N = $N", JlyConstant.JLY_DB_NAME, JlyConstant.JLY_DB_NAME)
                        .addStatement("this.$N = $N", JlyConstant.JLY_DAO_NAME, JlyConstant.JLY_DAO_NAME)
                .build()
        );

        // 构建方法
        typeBuilder.addMethods(buildMethodList(jlyDaoElement));

        if (jlyDaoElement != roomDaoElement) {
            typeBuilder.addMethods(buildMethodList(roomDaoElement));
        }


        // 构建java文件
        try {
            buildJavaFile(jlyDaoElement, typeBuilder.build()).writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeSpec.Builder buildTypeBuilder(TypeElement daoElement) {
        return TypeSpec.classBuilder(JlyUtils.getClassName(daoElement) + JlyConstant.JLY_DAO_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(JlyUtils.getPackageName(utils, daoElement), JlyUtils.getClassName(daoElement)));
    }

    private JavaFile buildJavaFile(TypeElement daoElement, TypeSpec typeSpec) {
        return JavaFile.builder(JlyUtils.getPackageName(utils, daoElement), typeSpec).build();
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

        JlyExec jlyExecSQL = element.getAnnotation(JlyExec.class);
        if (jlyExecSQL != null) {
            return new JlyExecProcessor(utils, filer, printer, element, jlyAopSet).process();
        }

        JlyInsert JlyInsert = element.getAnnotation(JlyInsert.class);
        if (JlyInsert != null) {
            return new JlyInsertProcessor(utils, filer, printer, element, jlyAopSet, JlyInsert).process();
        }

        JlyUpdate jlyUpdate = element.getAnnotation(JlyUpdate.class);
        if (jlyUpdate != null) {
            return new JlyUpdateProcessor(utils, filer, printer, element, jlyAopSet, jlyUpdate).process();
        }

        JlyDelete jlyDelete = element.getAnnotation(JlyDelete.class);
        if (jlyDelete != null) {
            return new JlyDeleteProcessor(utils, filer, printer, element, jlyAopSet, jlyDelete).process();
        }

        JlyQuery jlyQuery = element.getAnnotation(JlyQuery.class);
        if (jlyQuery != null) {
            return new JlyQueryProcessor(utils, filer, printer, element, jlyAopSet, roomEntitySet, jlyQuery).process();
        }

        JlyFlowQuery jlyFlowQuery = element.getAnnotation(JlyFlowQuery.class);
        if (jlyFlowQuery != null) {
            return new JlyFlowQueryProcessor(utils, filer, printer, element, jlyAopSet, roomEntitySet, jlyFlowQuery).process();
        }

        JlyFtsDelete jlyFtsDelete = element.getAnnotation(JlyFtsDelete.class);
        if (jlyFtsDelete != null) {
            return new JlyFtsDeleteProcessor(utils, filer, printer, element, jlyAopSet, jlyFtsDelete).process();
        }

        JlyFtsQuery jlyFtsQuery = element.getAnnotation(JlyFtsQuery.class);
        if (jlyFtsQuery != null) {
            return new JlyFtsQueryProcessor(utils, filer, printer, element, jlyAopSet, roomEntitySet, jlyFtsQuery).process();
        }

        return new JlyAopProcessor(utils, filer, printer, element, jlyAopSet).process();
    }

}


