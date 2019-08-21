package utils;

import function.annotation.base.GeneManager;
import function.external.flanking.FlankingCommand;
import global.Data;
import function.genotype.base.SampleManager;
import function.variant.base.VariantLevelFilterCommand;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 *
 * @author nick
 */
public class ThirdPartyToolManager {

    private static final String R_301_SCRIPT_SYSTEM_PATH = "/nfs/goldstein/software/R-3.0.1/bin/Rscript";
    private static final String R_325_SCRIPT_SYSTEM_PATH = "/nfs/goldstein/software/R-3.2.5/bin/Rscript";
    private static final String COLLAPSED_REGRESSION_R = "lib/collapsed_regression_2.0.R";
    private static final String PVALS_QQPLOT_R = "lib/pvals_qqplot.R";
    private static final String PERM_QQPLOT_FOR_COLLAPSING = "lib/generate_qq.py";
    private static final String PERL_SYSTEM_PATH = "perl";
    private static final String FLANKING_SEQ_PERL = "lib/flanking_seq.pl";
    private static final String TRIO_DENOVO_TIER = "lib/r0.5_trio_denovo_tier.R";
    private static final String TRIO_COMP_HET_TIER = "lib/r0.5_trio_comp_het_tier.R";
    private static final String NON_TRIO_TIER = "lib/non_trio_tier.R";
    public static final String PYTHON = "/nfs/goldstein/software/python2.7.7/bin/python";
    public static final String PLINK = "/nfs/goldstein/software/PLINK_1.90/3.38/plink";
    public static final String KING = "/nfs/goldstein/software/king_relatedness/king";
    private static final int nProcs = 4;

    public static int systemCall(String[] cmd) {
        LogManager.writeAndPrintNoNewLine("System call start");

        int exitValue = Data.NA;

        Vector<String> output = new Vector<>();

        try {
            Process myProc;

            if (cmd.length > 1) {
                LogManager.writeAndPrintNoNewLine(cmd[2]);
                myProc = Runtime.getRuntime().exec(cmd);
            } else {
                LogManager.writeAndPrintNoNewLine(cmd[0]);
                myProc = Runtime.getRuntime().exec(cmd[0]);
            }
            InputStream is = myProc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;

            while ((line = br.readLine()) != null) {
                output.add(line);
            }

            exitValue = myProc.waitFor();
        } catch (Exception e) {
            ErrorManager.send(e);
        }

        if (exitValue != 0) {
            LogManager.writeAndPrint("System call failed.");
            for (String str : output) {
                LogManager.writeAndPrint(str);
            }
        } else {
            LogManager.writeAndPrint("System call complete.");
        }

        return exitValue;
    }

    public static void callCollapsedRegression(String outputFile,
            String geneSampleMatrixFilePath,
            String method) {
        String cmd = R_301_SCRIPT_SYSTEM_PATH + " "
                + COLLAPSED_REGRESSION_R + " "
                + "--samples " + SampleManager.getTempCovarPath() + " "
                + "--clps " + geneSampleMatrixFilePath + " "
                + "--out " + outputFile + " "
                + "--method " + method + " "
                + "--transpose "
                + "--log " + CommonCommand.outputPath + "regress.log";

        int exitValue = systemCall(new String[]{cmd});

        if (exitValue != 0) {
            LogManager.writeAndPrint("\nwarning: the application failed to run Collapsed "
                    + "Regression script (" + method + "). \n");

            deleteFile(outputFile);
        }
    }

    public static void callFlankingSeq(String baseFlankingSeqFilePath) {
        String cmd = PERL_SYSTEM_PATH + " " + FLANKING_SEQ_PERL
                + " --variant " + VariantLevelFilterCommand.includeVariantId
                + " --width " + FlankingCommand.width
                + " --out " + baseFlankingSeqFilePath;

        int exitValue = systemCall(new String[]{cmd});

        if (exitValue != 0) {
            LogManager.writeAndPrint("\nwarning: the application failed to run flanking "
                    + "sequence script. \n");

            deleteFile(baseFlankingSeqFilePath);
        }
    }

    public static void callPvalueQQPlot(String pvalueFile, int col, String outputPath) {
        String cmd = R_301_SCRIPT_SYSTEM_PATH + " "
                + PVALS_QQPLOT_R + " "
                + pvalueFile + " "
                + col + " "
                + outputPath;

        int exitValue = systemCall(new String[]{cmd});

        if (exitValue != 0) {
            deleteFile(outputPath);
        }
    }

    public static void generatePvaluesQQPlot(String title, String pvalueName,
            String pvalueFile, String outputPath) {
        String[] temp = title.split(",");

        int col = 0;

        for (String str : temp) {
            col++;

            if (str.trim().equalsIgnoreCase(pvalueName)) {
                break;
            }
        }

        callPvalueQQPlot(pvalueFile, col, outputPath);
    }

    public static void generateQQPlot4CollapsingFetP(String summaryFilePath, String matrixFilePath, String outputPath) {
        int n = nProcs;

        // hack tweaks here, gene domain input usually too large, needs to review the code again
        if (GeneManager.hasGeneDomainInput()) {
            n = 1;
        }

        String cmd = PYTHON + " "
                + PERM_QQPLOT_FOR_COLLAPSING + " "
                + "--nprocs " + n + " "
                + summaryFilePath + " "
                + matrixFilePath + " "
                + outputPath; // output path

        int exitValue = systemCall(new String[]{cmd});

        if (exitValue != 0) {
            deleteFile(outputPath);
        }

    }

    private static void deleteFile(String filePath) {
        File f = new File(filePath);
        f.deleteOnExit();
    }

    public static void gzipFile(String path) {
        String cmd = "gzip -9 " + path;

        systemCall(new String[]{cmd});
    }

    public static void runTrioDenovoTier(String denovoFilePath) {
        String cmd = R_325_SCRIPT_SYSTEM_PATH + " "
                + TRIO_DENOVO_TIER + " "
                + denovoFilePath;

        systemCall(new String[]{cmd});
    }

    public static void runTrioCompHetTier(String compHetFilePath) {
        String cmd = R_325_SCRIPT_SYSTEM_PATH + " "
                + TRIO_COMP_HET_TIER + " "
                + compHetFilePath;

        systemCall(new String[]{cmd});
    }

    public static void runNonTrioTier(String variantFilePath) {
        String cmd = R_325_SCRIPT_SYSTEM_PATH + " "
                + NON_TRIO_TIER + " "
                + variantFilePath;

        systemCall(new String[]{cmd});
    }
}
