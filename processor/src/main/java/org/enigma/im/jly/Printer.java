package org.enigma.im.jly;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

public class Printer {

    private JavaFileObject javaFile;
    private StringBuffer buffer = new StringBuffer();

    public Printer(Filer filer, String file) {
//        try {
//            this.javaFile = filer.createSourceFile(file);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void i(String msg) {
        buffer.append(msg).append("\n");
    }

    public void i(String tag, String msg) {
        buffer.append(tag).append(":").append(msg).append("\n");
    }

    public void flush() {
//        try {
//            Writer writer = javaFile.openWriter();
//            writer.write(buffer.toString());
//            writer.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}
