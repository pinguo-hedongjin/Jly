package org.enigma.im.jly;

import android.content.Context;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * author:  hedongjin
 * date:  2019-06-03
 * description: Please contact me if you have any questions
 */
public class Jly {

    public static <J extends JlyRoomDatabase, R extends RoomDatabase> JlyRoomDatabase.Builder<J, R> databaseBuilder(
            Context context, Class<J> jClass, Class<R> rClass, String dbName) {
        return new JlyRoomDatabase.Builder(jClass, rClass, Room.databaseBuilder(context, rClass, dbName));
    }

    public static <J extends JlyRoomDatabase, R extends RoomDatabase> J getGeneratedImplementation(Class<J> jclass, String suffix, Class<R> rclass, R params) {
        final String fullPackage = jclass.getPackage().getName();
        String name = jclass.getCanonicalName();
        final String postPackageName = fullPackage.isEmpty()
                ? name
                : (name.substring(fullPackage.length() + 1));
        final String implName = postPackageName.replace('.', '_') + suffix;

        //noinspection TryWithIdenticalCatches
        try {

            @SuppressWarnings("unchecked")
            final Class<J> aClass = (Class<J>) Class.forName(fullPackage.isEmpty() ? implName : fullPackage + "." + implName);
            return aClass.getConstructor(rclass).newInstance(params);
        }  catch (Exception e) {
            throw new RuntimeException("Failed to create an instance of "
                    + jclass.getCanonicalName());
        }
    }

}
