package utils;

import com.github.lalyos.jfiglet.FigletFont;
import com.google.common.io.Files;
import function.external.base.DataManager;
import function.genotype.base.GenotypeLevelFilterCommand;
import global.Data;
import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nick
 */
public class LogManager {

    private static BufferedWriter userLog = null;

    public static String runTime;

    // users command log file path
    public static final String USERS_COMMAND_LOG = "log/users.command.log";
    // user sample file log path
    public static final String SAMPLE_DIR_LOG = "log/sample/";
    // program start date
    public static final Date date = new Date();

    public static void run() {
        logRunTime();

        logUserCommand();

        logSampleFile();

        close();
    }

    public static void initPath() {
        try {
            userLog = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    CommonCommand.outputPath + "atav.log")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LogManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        Data.userName = System.getProperty("user.name");
        try {
            writeAndPrint("Program start: " + date.toString());
            writeAndPrintNoNewLine(FigletFont.convertOneLine("ATAV"));
            writeAndPrint("Version: " + Data.VERSION);

            writeLog("ATAV command:");
            writeLog(CommandManager.command + "\n");

            userLog.flush();
        } catch (Exception e) {
            ErrorManager.print("Error in writing log file: " + e.toString());
        }
    }

    public static void writeAndPrint(String str) {
        System.out.println(str + "\n");
        writeLog(str + "\n");
    }

    public static void writeAndPrintNoNewLine(String str) {
        System.out.println(str);
        writeLog(str);
    }

    public static void writeLog(String str) {
        try {
            if (userLog != null) {
                userLog.write(str + "\n");
            }
        } catch (Exception e) {
        }
    }

    public static void close() {
        try {
            if (userLog != null) {
                userLog.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logRunTime() {
        long elapsedTime = RunTimeManager.getElapsedTime();

        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime);
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedTime);

        runTime = seconds + " seconds "
                + "(aka " + minutes + " minutes or "
                + hours + " hours)";

        writeAndPrint("\nTotal runtime: "
                + runTime + "\n");
    }

    private static void logUserCommand() {
        try {
            if (isBioinfoTeam()
                    || Data.VERSION.equals("trunk")
                    || Data.VERSION.equals("beta")) {
                return;
            }

            File file = new File(USERS_COMMAND_LOG);

            FileWriter fileWritter = new FileWriter(file, true);

            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);

            long outputFolderSize = folderSize(new File(CommonCommand.realOutputPath));

            bufferWritter.write(Data.userName + "\t"
                    + date.toString() + "\t"
                    + DBManager.getHost() + "\t"
                    + System.getenv("HOSTNAME") + "\t"
                    + CommandManager.command + "\t"
                    + runTime + "\t"
                    + outputFolderSize + " bytes");

            bufferWritter.newLine();

            bufferWritter.close();
        } catch (Exception e) { // not output error when writing to log file denied

        }
    }

    private static boolean isBioinfoTeam() throws Exception {
        InputStream input = new FileInputStream(Data.SYSTEM_CONFIG);
        Properties prop = new Properties();
        prop.load(input);

        String members = prop.getProperty("bioinfo-team");

        return members.contains(Data.userName);
    }

    private static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                length += file.length();
            } else {
                length += folderSize(file);
            }
        }
        return length;
    }

    private static void logSampleFile() {
        try {
            if (CommonCommand.isNonSampleAnalysis
                    || isBioinfoTeam()
                    || Data.VERSION.equals("trunk")
                    || Data.VERSION.contains("beta")) {
                return;
            }

            File sampleFile = new File(GenotypeLevelFilterCommand.sampleFile);

            File logSampleFile = new File(
                    SAMPLE_DIR_LOG
                    + Data.userName
                    + "."
                    + date.toString()
                    + "."
                    + sampleFile.getName());

            Files.copy(sampleFile, logSampleFile);
        } catch (Exception e) {
        }
    }

    public static void logExternalDataVersion() {
        String externalDataVersion = DataManager.getVersion();

        if (!externalDataVersion.isEmpty()) {
            writeAndPrintNoNewLine("External data version:");
            writeAndPrintNoNewLine(externalDataVersion);
        }
    }
}
