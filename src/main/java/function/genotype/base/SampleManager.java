package function.genotype.base;

import function.genotype.collapsing.CollapsingCommand;
import function.genotype.statistics.StatisticsCommand;
import function.variant.base.Variant;
import global.Data;
import utils.CommonCommand;
import utils.DBManager;
import utils.ErrorManager;
import utils.LogManager;
import java.io.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import utils.FormatManager;

/**
 *
 * @author nick
 */
public class SampleManager {

    // sample permission
    private static HashMap<String, String> sampleGroupMap // sample_name, group_name
            = new HashMap<String, String>();
    private static HashMap<String, HashSet<String>> userGroupMap // group_name, user set
            = new HashMap<String, HashSet<String>>();

    private static ArrayList<Sample> sampleList = new ArrayList<Sample>();
    private static HashMap<Integer, Sample> sampleMap = new HashMap<Integer, Sample>();

    private static int listSize; // case + ctrl
    private static int caseNum = 0;
    private static int ctrlNum = 0;

    // sample id StringBuilder is just temp used for creating temp tables
    private static StringBuilder allSampleIdSb = new StringBuilder();
    private static StringBuilder exomeSampleIdSb = new StringBuilder();
    private static StringBuilder genomeSampleIdSb = new StringBuilder();

    private static ArrayList<Sample> failedSampleList = new ArrayList<Sample>();
    private static ArrayList<Sample> diffTypeSampleList = new ArrayList<Sample>();
    private static ArrayList<Sample> notExistSampleList = new ArrayList<Sample>();

    private static ArrayList<Sample> deletedSampleList = new ArrayList<Sample>();
    private static ArrayList<Sample> replacedSampleList = new ArrayList<Sample>();

    private static ArrayList<Sample> restrictedSampleList = new ArrayList<Sample>();

    private static String tempCovarFile;
    private static String covariateFileTitle = "";

    public static void init() {
        if (CommonCommand.isNonSampleAnalysis) {
            return;
        }

        initSamplePermission();

        checkSampleFile();

        if (!GenotypeLevelFilterCommand.sampleFile.isEmpty()) {
            initFromSampleFile();
        } else if (GenotypeLevelFilterCommand.isAllSample) {
            initAllSampleFromAnnoDB();
        }

        initCovariate();

        initQuantitative();

        initSampleIndexAndSize();

        initTempTables();

        outputSampleListSummary();
    }

    private static void initSamplePermission() {
        initSampleGroup();

        initUserGroup();
    }

    private static void initSampleGroup() {
        try {
            File f = new File(Data.SAMPLE_GROUP_RESTRICTION_PATH);
            FileInputStream fstream = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String lineStr = "";
            while ((lineStr = br.readLine()) != null) {
                if (!lineStr.isEmpty()) {
                    String[] tmp = lineStr.trim().split("\t");

                    sampleGroupMap.put(tmp[0], tmp[1]);
                }
            }

            br.close();
            in.close();
            fstream.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static void initUserGroup() {
        try {
            File f = new File(Data.USER_GROUP_RESTRICTION_PATH);
            FileInputStream fstream = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String lineStr = "";
            while ((lineStr = br.readLine()) != null) {
                if (!lineStr.isEmpty()) {
                    String[] tmp = lineStr.trim().split("\t");

                    String groupName = tmp[0];
                    String[] users = tmp[1].split(",");

                    HashSet<String> userSet = userGroupMap.get(groupName);

                    if (userSet == null) {
                        userSet = new HashSet<String>();
                        userGroupMap.put(groupName, userSet);
                    }

                    for (String user : users) {
                        userSet.add(user);
                    }
                }
            }

            br.close();
            in.close();
            fstream.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static void checkSampleFile() {
        if (GenotypeLevelFilterCommand.sampleFile.isEmpty()
                && !GenotypeLevelFilterCommand.isAllSample) {
            ErrorManager.print("Please specify your sample file: --sample $PATH");
        }
    }

    private static void initSampleIndexAndSize() {
        int index = 0;

        for (Sample sample : sampleList) {
            sample.setIndex(index++);
        }

        listSize = sampleList.size();
    }

    private static void initAllSampleFromAnnoDB() {
        String sqlCode = "SELECT * FROM sample s, sample_pipeline_step p "
                + "WHERE s.sample_id = p.sample_id "
                + "AND p.pipeline_step_id = 10 "
                + "AND p.step_status = 'completed'";

        initSampleFromAnnoDB(sqlCode);
    }

    private static void initFromSampleFile() {
        String lineStr = "";
        int lineNum = 0;

        try {
            File f = new File(GenotypeLevelFilterCommand.sampleFile);
            FileInputStream fstream = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            while ((lineStr = br.readLine()) != null) {
                lineNum++;

                if (lineStr.isEmpty()) {
                    continue;
                }

                lineStr = lineStr.replaceAll("( )+", "");
                
                String[] values = lineStr.split("\t");

                String familyId = values[0];
                String individualId = values[1];
                String paternalId = values[2];
                String maternalId = values[3];
                
                int sex = Integer.valueOf(values[4]);
                if (sex != 1 && sex != 2) {
                    ErrorManager.print("\nWrong Sex value: " + sex 
                            + " (line " + lineNum+ " in sample file)");
                }

                double pheno = Double.valueOf(values[5]);
                if (pheno != 1 && pheno != 2) {
                    ErrorManager.print("\nWrong Phenotype value: " + pheno 
                            + " (line " + lineNum+ " in sample file)");
                }
                
                String sampleType = values[6];
                String captureKit = values[7];

                if (sampleType.equalsIgnoreCase("genome")) {
                    captureKit = "N/A";
                }

                int sampleId = getSampleId(individualId, sampleType, captureKit);

                if (sampleMap.containsKey(sampleId)) {
                    continue;
                }

                Sample sample = new Sample(sampleId, familyId, individualId,
                        paternalId, maternalId, sex, pheno, sampleType, captureKit);

                if (!checkSamplePermission(sample)) {
                    restrictedSampleList.add(sample);
                    continue;
                }

                if (sampleId == Data.NA) {
                    checkSampleList(sample);
                    continue;
                }

                sampleList.add(sample);
                sampleMap.put(sampleId, sample);

                countSampleNum(sample);
            }

            br.close();
            in.close();
            fstream.close();
        } catch (Exception e) {
            LogManager.writeAndPrintNoNewLine("\nError line ("
                    + lineNum + ") in sample file: " + lineStr);

            ErrorManager.send(e);
        }
    }

    private static void initSampleFromAnnoDB(String sqlCode) {
        try {
            ResultSet rs = DBManager.executeQuery(sqlCode);

            while (rs.next()) {
                int sampleId = rs.getInt("sample_id");

                String familyId = rs.getString("sample_name").trim();
                String individualId = rs.getString("sample_name").trim();
                String paternalId = "0";
                String maternalId = "0";
                String gender = rs.getString("gender").trim();

                int sex = 1; // M
                if (gender.equals("F")) {
                    sex = 2;
                }

                double pheno = 1;
                String sampleType = rs.getString("sample_type").trim();
                String captureKit = rs.getString("capture_kit").trim();

                Sample sample = new Sample(sampleId, familyId, individualId,
                        paternalId, maternalId, sex, pheno, sampleType, captureKit);

                if (!checkSamplePermission(sample)) {
                    restrictedSampleList.add(sample);
                    continue;
                }

                sampleList.add(sample);
                sampleMap.put(sampleId, sample);

                countSampleNum(sample);
            }

            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static boolean checkSamplePermission(Sample sample) {
        if (sampleGroupMap.containsKey(sample.getName())) {
            String groupName = sampleGroupMap.get(sample.getName());

            HashSet<String> userSet = userGroupMap.get(groupName);

            if (userSet.contains(Data.userName)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true; // not in sample restricted list
        }

    }

    private static void checkSampleList(Sample sample) {
        try {
            String sqlCode = "SELECT * FROM sample "
                    + "WHERE sample_name = '" + sample.getName() + "' "
                    + "AND sample_type = '" + sample.getType() + "' "
                    + "AND capture_kit = '" + sample.getCaptureKit() + "' "
                    + "AND sample_id IN (SELECT sample_id FROM sample_pipeline_step AS b "
                    + "WHERE pipeline_step_id = 10 AND step_status != 'completed')";

            ResultSet rs = DBManager.executeQuery(sqlCode);
            if (rs.next()) {
                failedSampleList.add(sample);
            } else {
                sqlCode = "SELECT * FROM sample "
                        + "WHERE sample_name = '" + sample.getName() + "' "
                        + "AND sample_id IN (SELECT sample_id FROM sample_pipeline_step AS b "
                        + "WHERE pipeline_step_id = 10 AND step_status = 'completed')";

                rs = DBManager.executeQuery(sqlCode);

                if (rs.next()) {
                    diffTypeSampleList.add(sample);
                } else {
                    notExistSampleList.add(sample);
                }
            }

            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static void outputSampleListSummary() {
        LogManager.writeAndPrint("The total number of samples included in the analysis is "
                + sampleList.size() + " (" + caseNum + " cases and " + ctrlNum + " controls).");

        printSampleList("The following samples are not allowed to use:",
                restrictedSampleList);

        printSampleList("The following samples are labeled as failed in AnnoDB:",
                failedSampleList);

        printSampleList("The following samples are in annodb but with a different seqtype or capture kit:",
                diffTypeSampleList);

        printSampleList("The following samples are not exist in AnnoDB:",
                notExistSampleList);
    }

    private static void printSampleList(String startMessage,
            ArrayList<Sample> sampleList) {
        if (!sampleList.isEmpty()) {
            LogManager.writeAndPrintWithoutNewLine(startMessage);

            for (Sample sample : sampleList) {
                LogManager.writeAndPrintWithoutNewLine(
                        FormatManager.getInteger(sample.getPrepId())
                        + "\t" + sample.getName()
                        + "\t" + sample.getType()
                        + "\t" + sample.getCaptureKit());
            }

            LogManager.writeAndPrintWithoutNewLine(""); // hack to add new line
        }
    }

    private static void initCovariate() {
        if (StatisticsCommand.covariateFile.isEmpty()) {
            return;
        }

        String lineStr = "";
        int lineNum = 0;

        try {
            File f = new File(StatisticsCommand.covariateFile);
            FileInputStream fstream = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            while ((lineStr = br.readLine()) != null) {
                lineNum++;

                if (lineStr.isEmpty()) {
                    continue;
                }

                if (covariateFileTitle.isEmpty()) {
                    covariateFileTitle = lineStr;
                }

                lineStr = lineStr.toLowerCase();
                String[] values = lineStr.split("\t");

                Sample sample = getSampleByName(values[1]);

                if (sample != null) {
                    sample.initCovariate(values);
                }
            }
        } catch (Exception e) {
            LogManager.writeAndPrintNoNewLine("\nError line ("
                    + lineNum + ") in covariate file: " + lineStr);

            ErrorManager.send(e);
        }

        resetSampleListByCovariate();
    }

    private static void resetSampleListByCovariate() {
        Iterator<Sample> it = sampleList.iterator();
        while (it.hasNext()) {
            Sample sample = it.next();
            if (sample.getCovariateList().isEmpty()) {
                it.remove();
                sampleMap.remove(sample.getId());
            }
        }
    }

    private static void initQuantitative() {
        if (StatisticsCommand.quantitativeFile.isEmpty()) {
            return;
        }

        String lineStr = "";
        int lineNum = 0;

        try {
            File f = new File(StatisticsCommand.quantitativeFile);
            FileInputStream fstream = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            while ((lineStr = br.readLine()) != null) {
                lineNum++;

                if (lineStr.isEmpty()) {
                    continue;
                }

                lineStr = lineStr.toLowerCase();
                String[] values = lineStr.split("\t");
                String name = values[0];
                double value = Double.valueOf(values[1]);

                Sample sample = getSampleByName(name);

                if (sample != null) {
                    sample.setQuantitativeTrait(value);
                }
            }
        } catch (Exception e) {
            LogManager.writeAndPrintNoNewLine("\nError line ("
                    + lineNum + ") in quantitative file: " + lineStr);

            ErrorManager.send(e);
        }

        resetSampleListByQuantitative();

        resetSamplePheno4Linear();
    }

    private static void resetSampleListByQuantitative() {
        Iterator<Sample> it = sampleList.iterator();
        while (it.hasNext()) {
            Sample sample = it.next();
            if (sample.getQuantitativeTrait() == Data.NA) {
                it.remove();
                sampleMap.remove(sample.getId());
            }
        }
    }

    public static void generateCovariateFile() {
        if (CollapsingCommand.isCollapsingDoLogistic
                || CollapsingCommand.isCollapsingDoLinear) {
            try {
                tempCovarFile = CommonCommand.outputPath + "covariate.txt";

                BufferedWriter bwCov = new BufferedWriter(new FileWriter(tempCovarFile));

                bwCov.write("Family" + "\t"
                        + "Sample" + "\t"
                        + "Pheno");

                String[] titles = covariateFileTitle.split("\t");

                for (int i = 2; i < titles.length; i++) {
                    bwCov.write("\t" + titles[i]);
                }

                bwCov.newLine();

                for (Sample sample : sampleList) {
                    bwCov.write(sample.getFamilyId() + "\t"
                            + sample.getName() + "\t");

                    if (CollapsingCommand.isCollapsingDoLogistic) {
                        bwCov.write(String.valueOf((int) (sample.getPheno() + 1)));
                    } else if (CollapsingCommand.isCollapsingDoLinear) {
                        bwCov.write(String.valueOf(sample.getQuantitativeTrait()));
                    }

                    for (String covar : sample.getCovariateList()) {
                        bwCov.write("\t" + covar);
                    }

                    bwCov.newLine();
                }

                bwCov.flush();
                bwCov.close();
            } catch (Exception e) {
                ErrorManager.send(e);
            }
        }
    }

    public static String getTempCovarPath() {
        return tempCovarFile;
    }

    private static Sample getSampleByName(String name) {
        for (Sample sample : sampleList) {
            if (sample.getName().equalsIgnoreCase(name)) {
                return sample;
            }
        }

        return null;
    }

    public static void recheckSampleList() {
        initDeletedAndReplacedSampleList();

        outputOutofDateSampleList(deletedSampleList, "Deleted");

        outputOutofDateSampleList(replacedSampleList, "Replaced");

        if (!deletedSampleList.isEmpty() || !replacedSampleList.isEmpty()) {
            LogManager.writeAndPrint("\nAlert: the data for the deleted/replaced "
                    + "sample used in the analysis is BAD data.");
        }
    }

    private static void initDeletedAndReplacedSampleList() {
        try {
            for (Sample sample : sampleList) {
                if (!sample.getName().startsWith("evs")) {
                    String time = getSampleFinishTime(sample.getId());

                    if (time.isEmpty()) {
                        deletedSampleList.add(sample);
                    } else if (!time.equals(sample.getFinishTime())) {
                        replacedSampleList.add(sample);
                    }
                }
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static void outputOutofDateSampleList(ArrayList<Sample> list, String name) {
        if (!list.isEmpty()) {
            LogManager.writeAndPrintNoNewLine("\n" + name
                    + " sample list from Annotation database during the analysis:\n");

            for (Sample sample : list) {
                LogManager.writeAndPrintNoNewLine(
                        sample.getName() + "\t"
                        + sample.getType() + "\t"
                        + sample.getCaptureKit());
            }
        }
    }

    private static void countSampleNum(Sample sample) {
        if (sample.isCase()) {
            caseNum++;
        } else {
            ctrlNum++;
        }
    }

    public static int getCaseNum() {
        return caseNum;
    }

    public static int getCtrlNum() {
        return ctrlNum;
    }

    private static void initTempTables() {
        createTempTables();

        initSampleIdSbs();

        insertSampleId2Tables();

        clearSampleIdSbs();
    }

    private static void createTempTables() {
        createTempTable(Data.ALL_SAMPLE_ID_TABLE);

        createTempTable(Data.GENOME_SAMPLE_ID_TABLE);

        createTempTable(Data.EXOME_SAMPLE_ID_TABLE);
    }

    private static void createTempTable(String sqlTable) {
        try {
            Statement stmt = DBManager.createStatement();

            String sqlQuery = "CREATE TEMPORARY TABLE "
                    + sqlTable
                    + "(id int, PRIMARY KEY (id)) ENGINE=TokuDB";

            stmt.executeUpdate(sqlQuery);
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    public static void initSampleIdSbs() {
        for (Sample sample : sampleList) {
            addToSampleIdSb(allSampleIdSb, sample.getId());

            if (sample.getType().equalsIgnoreCase("genome")) {
                addToSampleIdSb(genomeSampleIdSb, sample.getId());
            } else {
                addToSampleIdSb(exomeSampleIdSb, sample.getId());
            }
        }

        deleteLastComma(allSampleIdSb);
        deleteLastComma(genomeSampleIdSb);
        deleteLastComma(exomeSampleIdSb);
    }

    private static void addToSampleIdSb(StringBuilder sb, int id) {
        sb.append("(").append(id).append(")").append(",");
    }

    private static void deleteLastComma(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
    }

    private static void insertSampleId2Tables() {
        insertId2Table(allSampleIdSb.toString(), Data.ALL_SAMPLE_ID_TABLE);
        insertId2Table(genomeSampleIdSb.toString(), Data.GENOME_SAMPLE_ID_TABLE);
        insertId2Table(exomeSampleIdSb.toString(), Data.EXOME_SAMPLE_ID_TABLE);
    }

    private static void insertId2Table(String ids, String table) {
        try {
            if (!ids.isEmpty()) {
                DBManager.executeUpdate("INSERT INTO " + table + " VALUES " + ids);
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static void clearSampleIdSbs() {
        allSampleIdSb.setLength(0); // free memory
        genomeSampleIdSb.setLength(0);
        exomeSampleIdSb.setLength(0);
    }

    private static int getSampleId(String sampleName, String sampleType,
            String captureKit) throws Exception {
        int sampleId = Data.NA;

        try {
            String sqlCode = "SELECT sample_id FROM sample "
                    + "WHERE sample_name = '" + sampleName + "' "
                    + "AND sample_type = '" + sampleType + "' "
                    + "AND capture_kit = '" + captureKit + "' "
                    + "AND sample_id IN (SELECT sample_id FROM sample_pipeline_step AS b "
                    + "WHERE pipeline_step_id = 10 AND step_status = 'completed')";

            ResultSet rs = DBManager.executeQuery(sqlCode);
            if (rs.next()) {
                sampleId = rs.getInt("sample_id");
            }

            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }

        return sampleId;
    }

    public static int getSamplePrepId(int sampleId) {
        int prepId = Data.NA;

        try {
            String sqlCode = "SELECT prep_id FROM sample WHERE sample_id = " + sampleId;

            ResultSet rs = DBManager.executeReadOnlyQuery(sqlCode);
            if (rs.next()) {
                prepId = rs.getInt("prep_id");
            }

            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }

        return prepId;
    }

    public static String getSampleFinishTime(int sampleId) {
        String time = "";

        try {
            String sqlCode = "SELECT exec_finish_time FROM sample_pipeline_step "
                    + "WHERE pipeline_step_id = 10 AND step_status = 'completed' "
                    + "AND sample_id = " + sampleId;

            ResultSet rs = DBManager.executeReadOnlyQuery(sqlCode);
            if (rs.next()) {
                time = rs.getString("exec_finish_time");
            }

            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }

        return time;
    }

    public static int getIdByName(String sampleName) {
        for (Sample sample : sampleList) {
            if (sample.getName().equals(sampleName)) {
                return sample.getId();
            }
        }

        return Data.NA;
    }

    public static int getIndexById(int sampleId) {
        Sample sample = sampleMap.get(sampleId);

        if (sample != null) {
            return sample.getIndex();
        } else {
            return Data.NA;
        }
    }

    public static ArrayList<Sample> getList() {
        return sampleList;
    }

    public static HashMap<Integer, Sample> getMap() {
        return sampleMap;
    }

    public static int getListSize() {
        return listSize;
    }

    public static void initNonCarrierMap(Variant var,
            HashMap<Integer, Carrier> carrierMap,
            HashMap<Integer, NonCarrier> noncarrierMap) {
        ResultSet rs = null;
        String sql = "";

        int posIndex = var.getRegion().getStartPosition() % Data.COVERAGE_BLOCK_SIZE; // coverage data block size is 1024

        if (posIndex == 0) {
            posIndex = Data.COVERAGE_BLOCK_SIZE; // block boundary is ( ] 
        }

        int endPos = var.getRegion().getStartPosition() - posIndex + Data.COVERAGE_BLOCK_SIZE;

        try {
            for (int i = 0; i < Data.SAMPLE_TYPE.length; i++) {
                sql = "SELECT sample_id, min_coverage "
                        + "FROM " + Data.SAMPLE_TYPE[i]
                        + "_read_coverage_" + Data.COVERAGE_BLOCK_SIZE + "_chr"
                        + var.getRegion().getChrStr() + " c,"
                        + Data.SAMPLE_TYPE[i] + "_sample_id t "
                        + "WHERE c.position = " + endPos
                        + " AND c.sample_id = t.id";

                rs = DBManager.executeQuery(sql);
                while (rs.next()) {
                    NonCarrier noncarrier = new NonCarrier();

                    noncarrier.init(rs, posIndex);

                    if (!carrierMap.containsKey(noncarrier.getSampleId())) {

                        noncarrier.checkCoverageFilter(GenotypeLevelFilterCommand.minCaseCoverageNoCall,
                                GenotypeLevelFilterCommand.minCtrlCoverageNoCall);

                        noncarrier.checkValidOnXY(var);

                        if (noncarrier.getGenotype() != Data.NA) {
                            noncarrierMap.put(noncarrier.getSampleId(), noncarrier);
                        }
                    }
                }
            }

            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    public static void initCarrierMap(Variant var,
            HashMap<Integer, Carrier> carrierMap) {
        String sqlCarrier = "SELECT * "
                + "FROM called_" + var.getType() + " va,"
                + Data.ALL_SAMPLE_ID_TABLE + " t "
                + "WHERE va." + var.getType() + "_id = " + var.getVariantId()
                + " AND va.sample_id = t.id";

        ResultSet rs = null;
        try {
            rs = DBManager.executeQuery(sqlCarrier);

            while (rs.next()) {
                Carrier carrier = new Carrier();
                carrier.init(rs);

                carrier.checkCoverageFilter(GenotypeLevelFilterCommand.minCaseCoverageCall,
                        GenotypeLevelFilterCommand.minCtrlCoverageCall);

                carrier.checkQualityFilter();

                carrier.checkValidOnXY(var);

                carrierMap.put(carrier.getSampleId(), carrier);
            }
            rs.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    public static boolean isMale(int sampleId) {
        return sampleMap.get(sampleId).isMale();
    }

    private static void resetSamplePheno4Linear() {
        for (Sample sample : sampleList) {
            sample.setPheno(0);
        }

        ctrlNum = sampleList.size();
        caseNum = 0;
    }
}
