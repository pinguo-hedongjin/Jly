package org.enigma.im.jly;

import javax.annotation.processing.Filer;
import javax.lang.model.util.Elements;

/**
 * author:  hedongjin
 * date:  2019-06-11
 * description: Please contact me if you have any questions
 */
public abstract class BaseProcessor<T> implements IProcessor<T> {

    protected String TAG = this.getClass().getSimpleName();

    protected Elements utils;
    protected Filer filer;
    protected Printer printer;

    public BaseProcessor(Elements utils, Filer filer, Printer printer) {
        this.utils = utils;
        this.filer = filer;
        this.printer = printer;
    }
}
