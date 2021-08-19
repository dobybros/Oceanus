package oceanus.sdk.core.utils;


import java.util.List;

public class ValidateUtils {


    public static void checkListNotNull(List<?> param) {
        if(param == null || param.isEmpty())
            throw new IllegalArgumentException("Illegal params, param: " + param);
    }

    public static void checkAnyNotNull(Object... params) {
        for(Object it : params) {
            if(it != null)
                return;
        }
        throw new IllegalArgumentException("Illegal params, params: " + params);
    }
    public static void checkAllNotNull(Object... params) {
        for(Object it : params) {
            if(it == null)
                throw new IllegalArgumentException("Illegal params, params: " + params);
        }
    }
    public static void checkNotNull(Object param) {
        if(param == null)
            throw new IllegalArgumentException("Illegal params, param: " + param);
    }

    public static Object checkWithDefault(Object param, Object defaultValue) {
        if(param == null)
            return defaultValue;
        return param;
    }
}
