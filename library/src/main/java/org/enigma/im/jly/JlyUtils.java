package org.enigma.im.jly;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * author:  hedongjin
 * date:  2019-08-05
 * description: Please contact me if you have any questions
 */
public class JlyUtils {

    public static String formatList(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append("?");
            if (i < count - 1) {
                builder.append(",");
            }
        }

        return builder.toString();
    }

    public static String getInsertTable(Class cls, String methodName, Class[] typeList, Object[] valueList) {
        return (String) getTable(cls, JlyActionTable.class, methodName, typeList, valueList);
    }

    public static String[] getObserveTable(Class cls, String methodName, Class[] typeList, Object[] valueList) {
        Object result = getTable(cls, JlyObserveTable.class, methodName, typeList, valueList);

        if (result instanceof String) {
            return new String[]{(String) result};
        }

        if (result instanceof List) {
            return (String[]) ((List) result).toArray();
        }

        return (String[]) result;
    }

    public static Object getTable(Class cls, Class annotationCls, String methodName, Class[] typeList, Object[] valueList) {

        if (valueList.length == 0 || typeList.length != valueList.length) {
            throw new RuntimeException("type length = " + typeList.length + ", value length = " + valueList.length);
        }

        try {
            Method method = cls.getMethod(methodName, typeList);
            Annotation[][] annotations = method.getParameterAnnotations();

            for (int index = 0; index < typeList.length; index++) {
                if (contains(annotations[index], annotationCls)) {
                    return valueList[index];
                }
            }

            throw new RuntimeException("必须包含" + annotationCls.getSimpleName() + "注解字段");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean contains(Annotation[] annotations, Class annotationCls) {
        if (annotations == null) {
            return false;
        }

        for (Annotation _annotation : annotations) {
            if (_annotation.annotationType() == annotationCls) {
                return true;
            }
        }

        return false;
    }
}
