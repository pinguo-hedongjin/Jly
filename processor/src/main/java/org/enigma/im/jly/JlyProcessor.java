package org.enigma.im.jly;

import androidx.room.Dao;
import androidx.room.Entity;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
@AutoService(Processor.class)
public class JlyProcessor extends AbstractProcessor {

    private Elements utils;
    private Filer filer;
    private Printer printer;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        utils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        printer = new Printer(filer, "com.eni.Printer1");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {

        Set<String> supportSet = new LinkedHashSet<>();
        supportSet.add(JlyDatabase.class.getCanonicalName());
        supportSet.add(JlyDao.class.getCanonicalName());
        supportSet.add(JlyAop.class.getCanonicalName());
        supportSet.add(JlyEntity.class.getCanonicalName());
        supportSet.add(JlyConverter.class.getCanonicalName());

        supportSet.add(Dao.class.getCanonicalName());
        supportSet.add(Entity.class.getCanonicalName());

        return supportSet;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        try {
            printer.i(JlyConstant.TAG, "process start...");

            Set<TypeElement> jlyDbSet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(JlyDatabase.class);
            Set<TypeElement> jlyDaoSet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(JlyDao.class);
            Set<TypeElement> jlyAopSet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(JlyAop.class);
            Set<TypeElement> jlyEntitySet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(JlyEntity.class);
            Set<TypeElement> jlyConverterSet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(JlyConverter.class);

            Set<TypeElement> roomDaoSet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(Dao.class);
            Set<TypeElement> roomEntitySet = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(Entity.class);


            new JlyDatabaseProcessor(utils, filer, printer, jlyDbSet, jlyDaoSet, roomDaoSet).process();
            new JlyDaoProcessor(utils, filer, printer, jlyDaoSet, jlyAopSet, jlyConverterSet, roomDaoSet, zip(jlyEntitySet, roomEntitySet)).process();
            new JlyEntityParseProcessor(utils, filer, printer, zip(jlyEntitySet, roomEntitySet), jlyConverterSet).process();
            new JlyEntityInsertProcessor(utils, filer, printer, zip(jlyEntitySet, roomEntitySet), jlyConverterSet).process();

        } finally {
            printer.i(JlyConstant.TAG, "process end...");
            printer.flush();
        }

        return false;
    }

    private Set<TypeElement> zip(Set<TypeElement> s1, Set<TypeElement> s2) {
        Set<TypeElement> result = new HashSet<>();
        result.addAll(s1);
        result.addAll(s2);
        return result;
    }
}