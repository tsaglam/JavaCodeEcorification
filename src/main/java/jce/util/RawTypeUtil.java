package jce.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class that assist while dealing with raw types, for example when using pre-Java 5 APIs.
 * @author Timur Saglam
 */
public class RawTypeUtil {

    private RawTypeUtil() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Casts a raw {@link Collection} to a generic {@link List}. Can be used to avoid type safety problems when dealing
     * with raw lists.
     * @param clazz is the {@link Class} of the {@link Collection} entries.
     * @param collection is the raw {@link Collection}.
     * @return the generic {@link List}.
     */
    public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> collection) {
        List<T> genericList = new ArrayList<T>(collection.size());
        for (Object object : collection)
            genericList.add(clazz.cast(object));
        return genericList;
    }

}
