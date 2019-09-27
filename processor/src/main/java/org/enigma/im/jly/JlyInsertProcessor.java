package org.enigma.im.jly;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.Set;


/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
public class JlyInsertProcessor extends JlyAopProcessor {


    public JlyInsertProcessor(Elements utils, Filer filer, Printer printer, ExecutableElement element, Set<TypeElement> aopSet, JlyInsert insert) {
        super(utils, filer, printer, element, aopSet);
    }

    @Override
    protected void body(MethodSpec.Builder builder) {
        transaction(builder, () -> handleSql(builder));
    }

    private void handleSql(MethodSpec.Builder builder) {

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


        builder.addStatement("$T $N = $T.getInsertTable(getClass(), $S, new $T[]{$N}, new $T[]{$N})",
                String.class, JlyConstant.JLY_TABLE_NAME, JlyConstant.JLY_LIB_UTILS, element.getSimpleName().toString(),
                Class.class, typeList.toString(), Object.class, valueList.toString());

        Pair<ClassName, String> type_name = getInsetPair();
        builder.addStatement("new $T($N, $N).insert($N)", type_name.fst, JlyConstant.JLY_DB_NAME,
                JlyConstant.JLY_TABLE_NAME, type_name.snd);
    }

    private Pair<ClassName, String> getInsetPair() {
        for (VariableElement e : element.getParameters()) {
            if (!JlyUtils.containJlyActionTable(e)) {
                String name = e.getSimpleName().toString();
                if (JlyUtils.isList(e)) {
                    return new Pair<>(JlyUtils.get(JlyUtils.getListType(e.asType()) + JlyConstant.JLY_INSERT_SUFFIX), name);
                }
                if (JlyUtils.isArray(e)) {
                    return new Pair<>(JlyUtils.get(JlyUtils.getArrayType(e.asType()) + JlyConstant.JLY_INSERT_SUFFIX), name);
                }
                return new Pair<>(JlyUtils.get(e.asType().toString() + JlyConstant.JLY_INSERT_SUFFIX), name);
            }
        }

        throw new RuntimeException("没有需要插入的数据");

    }

    public class Pair<A, B> {
        public final A fst;
        public final B snd;

        public Pair(A var1, B var2) {
            this.fst = var1;
            this.snd = var2;
        }
    }
}
