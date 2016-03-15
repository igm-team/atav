package function.genotype.collapsing;

import function.annotation.base.GeneManager;
import function.genotype.base.GenotypeLevelFilterCommand;
import function.genotype.statistics.StatisticsCommand;
import global.Data;
import java.util.Iterator;
import static utils.CommandManager.checkValueValid;
import static utils.CommandManager.getValidDouble;
import static utils.CommandManager.getValidInteger;
import static utils.CommandManager.getValidPath;
import utils.CommandOption;

/**
 *
 * @author nick
 */
public class CollapsingCommand {

    public static boolean isCollapsingSingleVariant = false;
    public static boolean isCollapsingCompHet = false;
    public static boolean isRecessive = false;
    public static String coverageSummaryFile = "";
    public static double looMaf = Data.NO_FILTER;
    public static double looCombFreq = Data.NO_FILTER;
    public static boolean isCollapsingDoLinear = false;
    public static boolean isCollapsingDoLogistic = false;
    public static String regionBoundaryFile = "";

    public static void initSingleVarOptions(Iterator<CommandOption> iterator)
            throws Exception { // collapsing dom or rec
        CommandOption option;

        while (iterator.hasNext()) {
            option = (CommandOption) iterator.next();
            if (option.getName().equals("--loo-maf")) {
                checkValueValid(0.5, 0, option);
                looMaf = getValidDouble(option);
            } else if (option.getName().equals("--loo-maf-rec")
                    || option.getName().equals("--loo-maf-recessive")) {
                checkValueValid(0.5, 0, option);
                GenotypeLevelFilterCommand.maf4Recessive = getValidDouble(option);
            } else if (option.getName().equals("--loo-mhgf-rec")
                    || option.getName().equals("--loo-mhgf-recessive")) {
                checkValueValid(0.5, 0, option);
                GenotypeLevelFilterCommand.mhgf4Recessive = getValidDouble(option);
            } else if (option.getName().equals("--read-coverage-summary")) {
                coverageSummaryFile = getValidPath(option);
                GeneManager.initCoverageSummary();
            } else if (option.getName().equals("--covariate")) {
                StatisticsCommand.covariateFile = getValidPath(option);
                isCollapsingDoLogistic = true;
            } else if (option.getName().equals("--quantitative")) {
                StatisticsCommand.quantitativeFile = getValidPath(option);
                isCollapsingDoLinear = true;
            } else if (option.getName().equals("--min-hom-case-rec")) {
                checkValueValid(Data.NO_FILTER, 0, option);
                StatisticsCommand.minHomCaseRec = getValidInteger(option);
            } else if (option.getName().equals("--region-boundary")) {
                regionBoundaryFile = getValidPath(option);
            } else {
                continue;
            }

            iterator.remove();
        }

        if (isCollapsingDoLinear) {
            isCollapsingDoLogistic = false;
        }
    }

    public static void initCompHetOptions(Iterator<CommandOption> iterator)
            throws Exception { // collapsing comp het
        CommandOption option;

        while (iterator.hasNext()) {
            option = (CommandOption) iterator.next();
            if (option.getName().equals("--loo-maf")) {
                checkValueValid(0.5, 0, option);
                looMaf = getValidDouble(option);
            } else if (option.getName().equals("--loo-maf-rec")
                    || option.getName().equals("--loo-maf-recessive")) {
                checkValueValid(0.5, 0, option);
                GenotypeLevelFilterCommand.maf4Recessive = getValidDouble(option);
            } else if (option.getName().equals("--loo-mhgf-rec")
                    || option.getName().equals("--loo-mhgf-recessive")) {
                checkValueValid(0.5, 0, option);
                GenotypeLevelFilterCommand.mhgf4Recessive = getValidDouble(option);
            } else if (option.getName().equals("--loo-comb-freq")) {
                checkValueValid(1, 0, option);
                looCombFreq = getValidDouble(option);
            } else if (option.getName().equals("--read-coverage-summary")) {
                coverageSummaryFile = getValidPath(option);
                GeneManager.initCoverageSummary();
            } else if (option.getName().equals("--covariate")) {
                StatisticsCommand.covariateFile = getValidPath(option);
                isCollapsingDoLogistic = true;
            } else if (option.getName().equals("--quantitative")) {
                StatisticsCommand.quantitativeFile = getValidPath(option);
                isCollapsingDoLinear = true;
            } else {
                continue;
            }

            iterator.remove();
        }

        if (isCollapsingDoLinear) {
            isCollapsingDoLogistic = false;
        }
    }

    public static boolean isLooCombFreqValid(double value) {
        if (looCombFreq == Data.NO_FILTER) {
            return true;
        }

        if (value <= looCombFreq) {
            return true;
        }

        return false;
    }
}
