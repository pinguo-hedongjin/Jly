package org.enigma.im.jly;

import androidx.annotation.NonNull;

/**
 * author:  hedongjin
 * date:  2019-06-04
 * description: Please contact me if you have any questions
 */
public interface IJlyAop {
    boolean isPointcut(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] args);
    void before(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] args);
    void after(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] args);
}
