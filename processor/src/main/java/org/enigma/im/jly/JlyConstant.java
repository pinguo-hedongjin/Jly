package org.enigma.im.jly;

import com.squareup.javapoet.ClassName;

/**
 * author:  hedongjin
 * date:  2019-06-10
 * description: Please contact me if you have any questions
 */
public class JlyConstant {

    public static String TAG = "Jly";
    public static String ROOM_DAO_SUFFIX = "_Impl";
    public static String JLY_DAO_SUFFIX = "_Jly";
    public static String JLY_PARSE_SUFFIX = "_Parse";
    public static String JLY_PARSE_METHOD = "parse";
    public static String JLY_INSERT_SUFFIX = "_Insert";
    public static String JLY_DAO_NAME = "dao";
    public static String JLY_AOP_PREFIX = "aop";
    public static String JLY_INIT_NAME = "<init>";
    public static String JLY_SQL_NAME = "sql";
    public static String JLY_OFFSET_NAME = "offset";
    public static String JLY_COUNT_NAME = "count";
    public static String JLY_RESULT_NAME = "result";
    public static String JLY_ITEM_NAME = "item";
    public static String JLY_VALUE_NAME = "value";

    public static String JLY_TABLE_NAME = "tableName";
    public static ClassName JLY_INSERT_TYPE = ClassName.get("androidx.room", "EntityInsertionAdapter");

    public static String JLY_STMT_NAME = "stmt";
    public static ClassName JLY_STMT_TYPE = ClassName.get("androidx.sqlite.db", "SupportSQLiteStatement");

    public static String JLY_CURSOR_NAME = "cursor";
    public static ClassName JLY_CURSOR_TYPE = ClassName.get("android.database", "Cursor");

    public static String JLY_QUERY_NAME = "stmt";
    public static ClassName JLY_QUERY_TYPE = ClassName.get("androidx.room", "RoomSQLiteQuery");

    public static String JLY_DB_NAME = "db";
    public static ClassName JLY_DB_TYPE = ClassName.get("androidx.room", "RoomDatabase");

    public static ClassName JLY_LIB_UTILS = ClassName.get("org.enigma.im.jly", "JlyUtils");
    public static ClassName JLY_RXROOM_TYPE = ClassName.get("androidx.room", "RxRoom");

    public static String JLY_RXROOM_START = "return $T.createFlowable($N, $N, new $T<$N>() {\n" +
            "      @Override\n" +
            "      public $N call() throws Exception {";

    public static String JLY_RXROOM_END = "@Override\n" +
            "      protected void finalize() {\n" +
            "        $N.release();\n" +
            "      }\n" +
            "    });";
}
