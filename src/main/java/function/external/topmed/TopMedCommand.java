package function.external.topmed;

import global.Data;

/**
 *
 * @author nick
 */
public class TopMedCommand {

    public static boolean isList = false;
    public static boolean isInclude = false;
    public static float maxAF = Data.NO_FILTER;

    public static boolean isMaxAFValid(float value) {
        if (maxAF == Data.NO_FILTER) {
            return true;
        }

        return value <= maxAF
                || value == Data.FLOAT_NA;
    }
}