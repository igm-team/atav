package function.external.exac;

import global.Data;

/**
 *
 * @author nick
 */
public class ExacCommand {

    public static boolean isListExac = false;
    public static boolean isIncludeExac = false;

    public static String exacPop = "global";
    public static float exacMaf = Data.NO_FILTER;
    public static float exacVqslodSnv = Data.NO_FILTER;
    public static float exacVqslodIndel = Data.NO_FILTER;
    public static float exacMeanCoverage = Data.NO_FILTER;

    public static boolean isExacMafValid(float value) {
        if (exacMaf == Data.NO_FILTER) {
            return true;
        }

        return value <= exacMaf
                || value == Data.NA;
    }

    public static boolean isExacVqslodValid(float value, boolean isSnv) {
        if (isSnv) {
            return isExacVqslodSnvValid(value);
        } else {
            return isExacVqslodIndelValid(value);
        }
    }

    private static boolean isExacVqslodSnvValid(float value) {
        if (exacVqslodSnv == Data.NO_FILTER) {
            return true;
        }

        return value >= exacVqslodSnv
                || value == Data.NA;
    }

    private static boolean isExacVqslodIndelValid(float value) {
        if (exacVqslodIndel == Data.NO_FILTER) {
            return true;
        }

        return value >= exacVqslodIndel
                || value == Data.NA;
    }

    public static boolean isExacMeanCoverageValid(float value) {
        if (exacMeanCoverage == Data.NO_FILTER) {
            return true;
        }

        return value >= exacMeanCoverage;
    }
}
