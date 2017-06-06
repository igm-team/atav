package function.genotype.base;

import global.Data;
import java.util.ArrayList;
import java.util.Iterator;
import static utils.CommandManager.checkRangeListValid;
import static utils.CommandManager.checkRangeValid;
import static utils.CommandManager.getValidDouble;
import static utils.CommandManager.getValidInteger;
import static utils.CommandManager.getValidPath;
import static utils.CommandManager.getValidRange;
import static utils.CommandManager.getValidRangeList;
import static utils.CommandManager.getValidFloat;
import utils.CommandOption;
import static utils.CommandManager.checkValueValid;
import static utils.CommandManager.checkValueValid;
import static utils.CommandManager.checkValueValid;
import static utils.CommandManager.checkValueValid;

/**
 *
 * @author nick
 */
public class GenotypeLevelFilterCommand {

    public static String sampleFile = "";
    public static boolean isAllSample = false;
    public static boolean isAllNonRef = false;
    public static boolean isAllGeno = false;
    public static double maxCtrlMaf = Data.NO_FILTER;
    public static double minCtrlMaf = Data.NO_FILTER;
    public static int minCoverage = Data.NO_FILTER;
    public static int minCaseCoverageCall = Data.NO_FILTER;
    public static int minCaseCoverageNoCall = Data.NO_FILTER;
    public static int minCtrlCoverageCall = Data.NO_FILTER;
    public static int minCtrlCoverageNoCall = Data.NO_FILTER;
    public static int minVarPresent = 1; // special case
    public static int minCaseCarrier = Data.NO_FILTER;
    public static String[] varStatus; // null: no filer or all
    public static ArrayList<double[]> hetPercentAltReadList = null;
    public static double[] homPercentAltRead = null;
    public static float snvGQ = Data.NO_FILTER;
    public static float indelGQ = Data.NO_FILTER;
    public static float snvFS = Data.NO_FILTER;
    public static float indelFS = Data.NO_FILTER;
    public static float snvHapScore = Data.NO_FILTER;
    public static float indelHapScore = Data.NO_FILTER;
    public static float snvMQ = Data.NO_FILTER;
    public static float indelMQ = Data.NO_FILTER;
    public static float snvQD = Data.NO_FILTER;
    public static float indelQD = Data.NO_FILTER;
    public static float snvQual = Data.NO_FILTER;
    public static float indelQual = Data.NO_FILTER;
    public static float snvRPRS = Data.NO_FILTER;
    public static float indelRPRS = Data.NO_FILTER;
    public static float snvMQRS = Data.NO_FILTER;
    public static float indelMQRS = Data.NO_FILTER;
    public static boolean isQcMissingIncluded = false;
    public static int maxQcFailSample = Data.NO_FILTER;
    public static float maxHetBinomialP = Data.NO_FILTER;
    public static float hetBinomialProbability = 0.5f;
    public static float maxHomBinomialP = Data.NO_FILTER;
    public static float homBinomialProbability = 0.01f;
    public static boolean disableCheckOnSexChr = false;
    public static final String[] VARIANT_STATUS = {"pass", "pass+intermediate", "all"};
    public static double minCoveredSampleBinomialP = Data.NO_FILTER;

    public static void initOptions(Iterator<CommandOption> iterator)
            throws Exception {
        CommandOption option;

        while (iterator.hasNext()) {
            option = (CommandOption) iterator.next();
            switch (option.getName()) {
                case "--sample":
                case "--pedinfo":
                    sampleFile = getValidPath(option);
                    break;
                case "--all-sample":
                    isAllSample = true;
                    break;
                case "--all-non-ref":
                    isAllNonRef = true;
                    break;
                case "--all-geno":
                    isAllGeno = true;
                    break;
                case "--ctrlMAF":
                case "--ctrl-maf":
                case "--max-ctrl-maf":
                    checkValueValid(0.5, 0, option);
                    maxCtrlMaf = getValidDouble(option);
                    break;
                case "--min-ctrl-maf":
                    checkValueValid(0.5, 0, option);
                    minCtrlMaf = getValidDouble(option);
                    break;
                case "--min-coverage":
                    checkValueValid(new String[]{"0", "3", "10", "20", "201"}, option);
                    minCoverage = getValidInteger(option);
                    break;
                case "--min-case-coverage-call":
                    checkValueValid(Data.NO_FILTER, 0, option);
                    minCaseCoverageCall = getValidInteger(option);
                    break;
                case "--min-case-coverage-no-call":
                    checkValueValid(new String[]{"3", "10", "20", "201"}, option);
                    minCaseCoverageNoCall = getValidInteger(option);
                    break;
                case "--min-ctrl-coverage-call":
                    checkValueValid(Data.NO_FILTER, 0, option);
                    minCtrlCoverageCall = getValidInteger(option);
                    break;
                case "--min-ctrl-coverage-no-call":
                    checkValueValid(new String[]{"3", "10", "20", "201"}, option);
                    minCtrlCoverageNoCall = getValidInteger(option);
                    break;
                case "--min-variant-present":
                    checkValueValid(Data.NO_FILTER, 0, option);
                    minVarPresent = getValidInteger(option);
                    break;
                case "--min-case-carrier":
                    checkValueValid(Data.NO_FILTER, 0, option);
                    minCaseCarrier = getValidInteger(option);
                    break;
                case "--var-status":
                    checkValueValid(VARIANT_STATUS, option);
                    String str = option.getValue().replace("+", ",");
                    if (str.contains("all")) {
                        varStatus = null;
                    } else {
                        varStatus = str.split(",");
                    }
                    break;
                case "--het-percent-alt-read":
                    checkRangeListValid("0-1", option);
                    hetPercentAltReadList = getValidRangeList(option);
                    break;
                case "--hom-percent-alt-read":
                    checkRangeValid("0-1", option, option.getValue());
                    homPercentAltRead = getValidRange(option);
                    break;
                case "--gq":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvGQ = getValidFloat(option);
                    indelGQ = getValidFloat(option);
                    break;
                case "--snv-gq":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvGQ = getValidFloat(option);
                    break;
                case "--indel-gq":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelGQ = getValidFloat(option);
                    break;
                case "--fs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvFS = getValidFloat(option);
                    indelFS = getValidFloat(option);
                    break;
                case "--snv-fs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvFS = getValidFloat(option);
                    break;
                case "--indel-fs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelFS = getValidFloat(option);
                    break;
                case "--hap-score":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvHapScore = getValidFloat(option);
                    indelHapScore = getValidFloat(option);
                    break;
                case "--snv-hap-score":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvHapScore = getValidFloat(option);
                    break;
                case "--indel-hap-score":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelHapScore = getValidFloat(option);
                    break;
                case "--mq":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvMQ = getValidFloat(option);
                    indelMQ = getValidFloat(option);
                    break;
                case "--snv-mq":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvMQ = getValidFloat(option);
                    break;
                case "--indel-mq":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelMQ = getValidFloat(option);
                    break;
                case "--qd":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvQD = getValidFloat(option);
                    indelQD = getValidFloat(option);
                    break;
                case "--snv-qd":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvQD = getValidFloat(option);
                    break;
                case "--indel-qd":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelQD = getValidFloat(option);
                    break;
                case "--qual":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvQual = getValidFloat(option);
                    indelQual = getValidFloat(option);
                    break;
                case "--snv-qual":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvQual = getValidFloat(option);
                    break;
                case "--indel-qual":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelQual = getValidFloat(option);
                    break;
                case "--rprs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvRPRS = getValidFloat(option);
                    indelRPRS = getValidFloat(option);
                    break;
                case "--snv-rprs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvRPRS = getValidFloat(option);
                    break;
                case "--indel-rprs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelRPRS = getValidFloat(option);
                    break;
                case "--mqrs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvMQRS = getValidFloat(option);
                    indelMQRS = getValidFloat(option);
                    break;
                case "--snv-mqrs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    snvMQRS = getValidFloat(option);
                    break;
                case "--indel-mqrs":
                    checkValueValid(Data.NO_FILTER, Data.NO_FILTER, option);
                    indelMQRS = getValidFloat(option);
                    break;
                case "--include-qc-missing":
                    isQcMissingIncluded = true;
                    break;
                case "--max-qc-fail-sample":
                    checkValueValid(Data.NO_FILTER, 0, option);
                    maxQcFailSample = getValidInteger(option);
                    break;
                case "--max-het-binomial-p":
                    checkValueValid(1, 0, option);
                    maxHetBinomialP = getValidFloat(option);
                    break;
                case "--het-binomial-probability":
                    checkValueValid(1, 0, option);
                    hetBinomialProbability = getValidFloat(option);
                    break;
                case "--max-hom-binomial-p":
                    checkValueValid(1, 0, option);
                    maxHomBinomialP = getValidFloat(option);
                    break;
                case "--hom-binomial-probability":
                    checkValueValid(1, 0, option);
                    homBinomialProbability = getValidFloat(option);
                    break;
                case "--disable-check-on-sex-chr":
                    disableCheckOnSexChr = true;
                    break;
                case "--min-covered-sample-binomial-p":
                    checkValueValid(Data.NO_FILTER, 0, option);
                    minCoveredSampleBinomialP = getValidDouble(option);
                    break;
                default:
                    continue;
            }

            iterator.remove();
        }

        initMinCoverage();
    }

    private static void initMinCoverage() {
        if (minCoverage != Data.NO_FILTER) {
            if (minCaseCoverageCall == Data.NO_FILTER) {
                minCaseCoverageCall = minCoverage;
            }

            if (minCaseCoverageNoCall == Data.NO_FILTER) {
                minCaseCoverageNoCall = minCoverage;
            }

            if (minCtrlCoverageCall == Data.NO_FILTER) {
                minCtrlCoverageCall = minCoverage;
            }

            if (minCtrlCoverageNoCall == Data.NO_FILTER) {
                minCtrlCoverageNoCall = minCoverage;
            }
        }
    }

    public static boolean isMaxCtrlMafValid(double value) {
        if (maxCtrlMaf == Data.NO_FILTER) {
            return true;
        }

        return value <= maxCtrlMaf;
    }

    public static boolean isMinCtrlMafValid(double value) {
        if (minCtrlMaf == Data.NO_FILTER) {
            return true;
        }

        return value >= minCtrlMaf;
    }

    public static boolean isMinCoverageValid(int value, int minCov) {
        if (minCov == Data.NO_FILTER) {
            return true;
        }

        return value >= minCov;
    }

    public static boolean isMinVarPresentValid(int value) {
        if (minVarPresent == Data.NO_FILTER) {
            return true;
        }

        return value >= minVarPresent;
    }

    public static boolean isMinCaseCarrierValid(int value) {
        if (minCaseCarrier == Data.NO_FILTER) {
            return true;
        }

        return value >= minCaseCarrier;
    }

    public static boolean isVarStatusValid(String value) {
        if (varStatus == null) { // no filter or all
            return true;
        }

        if (value == null) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else {
            for (String str : varStatus) {
                if (value.equals(str)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isGqValid(float value, boolean isSnv) {
        if (isSnv) {
            return isGqValid(value, snvGQ);
        } else {
            return isGqValid(value, indelGQ);
        }
    }

    private static boolean isGqValid(float value, float gq) {
        if (gq == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value >= gq) {
            return true;
        }

        return false;
    }

    public static boolean isFsValid(float value, boolean isSnv) {
        if (isSnv) {
            return isFsValid(value, snvFS);
        } else {
            return isFsValid(value, indelFS);
        }
    }

    private static boolean isFsValid(float value, float strandBiasFS) {
        if (strandBiasFS == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value <= strandBiasFS) {
            return true;
        }

        return false;
    }

    public static boolean isHapScoreValid(float value, boolean isSnv) {
        if (isSnv) {
            return isHapScoreValid(value, snvHapScore);
        } else {
            return isHapScoreValid(value, indelHapScore);
        }
    }

    private static boolean isHapScoreValid(float value, float hapScore) {
        if (hapScore == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value <= hapScore) {
            return true;
        }

        return false;
    }

    public static boolean isMqValid(float value, boolean isSnv) {
        if (isSnv) {
            return isMqValid(value, snvMQ);
        } else {
            return isMqValid(value, indelMQ);
        }
    }

    private static boolean isMqValid(float value, float rmsMapQualMQ) {
        if (rmsMapQualMQ == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value >= rmsMapQualMQ) {
            return true;
        }

        return false;
    }

    public static boolean isQdValid(float value, boolean isSnv) {
        if (isSnv) {
            return isQdValid(value, snvQD);
        } else {
            return isQdValid(value, indelQD);
        }
    }

    public static boolean isQdValid(float value, float qualByDepthQD) {
        if (qualByDepthQD == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value >= qualByDepthQD) {
            return true;
        }

        return false;
    }

    public static boolean isQualValid(float value, boolean isSnv) {
        if (isSnv) {
            return isQualValid(value, snvQual);
        } else {
            return isQualValid(value, indelQual);
        }
    }

    private static boolean isQualValid(float value, float qual) {
        if (qual == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value >= qual) {
            return true;
        }

        return false;
    }

    public static boolean isRprsValid(float value, boolean isSnv) {
        if (isSnv) {
            return isRprsValid(value, snvRPRS);
        } else {
            return isRprsValid(value, indelRPRS);
        }
    }

    public static boolean isRprsValid(float value, float readPosRankSum) {
        if (readPosRankSum == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value >= readPosRankSum) {
            return true;
        }

        return false;
    }

    public static boolean isMqrsValid(float value, boolean isSnv) {
        if (isSnv) {
            return isMqrsValid(value, snvMQRS);
        } else {
            return isMqrsValid(value, indelMQRS);
        }
    }

    public static boolean isMqrsValid(float value, float mapQualRankSum) {
        if (mapQualRankSum == Data.NO_FILTER) {
            return true;
        }

        if (value == Data.NA) {
            if (isQcMissingIncluded) {
                return true;
            }
        } else if (value >= mapQualRankSum) {
            return true;
        }

        return false;
    }

    public static boolean isHetPercentAltReadValid(double value) {
        if (hetPercentAltReadList == null) {
            return true;
        }

        if (value != Data.NA) {
            for (double[] range : hetPercentAltReadList) {
                if (value >= range[0]
                        && value <= range[1]) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isHomPercentAltReadValid(double value) {
        if (homPercentAltRead == null) {
            return true;
        }

        if (value != Data.NA) {
            if (value >= homPercentAltRead[0]
                    && value <= homPercentAltRead[1]) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMaxQcFailSampleValid(int value) {
        if (maxQcFailSample == Data.NO_FILTER) {
            return true;
        }

        return value <= maxQcFailSample;
    }

    public static boolean isMaxHetBinomialPValid(double value) {
        if (maxHetBinomialP == Data.NO_FILTER) {
            return true;
        }

        return value <= maxHetBinomialP;
    }

    public static boolean isMaxHomBinomialPValid(double value) {
        if (maxHomBinomialP == Data.NO_FILTER) {
            return true;
        }

        return value <= maxHomBinomialP;
    }

    public static boolean isMinCoveredSampleBinomialPValid(double value) {
        if (minCoveredSampleBinomialP == Data.NO_FILTER) {
            return true;
        }

        return value >= minCoveredSampleBinomialP;
    }
}
