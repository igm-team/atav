package function.cohort.collapsing;

import function.annotation.base.GeneManager;
import function.cohort.base.Sample;
import function.cohort.base.SampleManager;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import utils.CommonCommand;
import utils.ErrorManager;
import utils.LogManager;
import utils.ThirdPartyToolManager;

/**
 *
 * @author nick
 */
public class CollapsingLite {

    private static BufferedWriter bwSampleMatrix = null;
    private static BufferedWriter bwSummary = null;

    private static final String genotypeFilePath = CommonCommand.outputPath + "genotypes.csv";
    private static final String matrixFilePath = CommonCommand.outputPath + "matrix.txt";
    private static final String summaryFilePath = CommonCommand.outputPath + "summary.csv";
    private static final String geneFetPQQPlotPath = CommonCommand.outputPath + "summary.fet.p.qq.plot.pdf";

    private static final String VARIANT_ID_HEADER = "Variant ID";
    private static final String ALL_ANNOTATION_HEADER = "All Effect Gene Transcript HGVS_p Polyphen_Humdiv Polyphen_Humvar";
    private static final String SAMPLE_NAME_HEADER = "Sample Name";

    private static final String[] HEADERS = {
        VARIANT_ID_HEADER,
        ALL_ANNOTATION_HEADER,
        SAMPLE_NAME_HEADER
    };

    private static ArrayList<CollapsingSummary> summaryList = new ArrayList<>();
    private static HashMap<String, CollapsingSummary> summaryMap = new HashMap<>();
    
    public static void initInput() {
        ThirdPartyToolManager.copyFile(CollapsingCommand.genotypeFile, genotypeFilePath);
    }

    public static void initOutput() {
        try {
            bwSampleMatrix = new BufferedWriter(new FileWriter(matrixFilePath));
            bwSummary = new BufferedWriter(new FileWriter(summaryFilePath));

            bwSampleMatrix.write("sample/gene" + "\t");
            bwSummary.write(CollapsingGeneSummary.getTitle());
            bwSummary.newLine();

            for (Sample sample : SampleManager.getList()) {
                bwSampleMatrix.write(sample.getName() + "\t");
            }
            bwSampleMatrix.newLine();
        } catch (IOException ex) {
            ErrorManager.send(ex);
        }
    }

    public static void closeOutput() {
        try {
            bwSummary.flush();
            bwSummary.close();
        } catch (IOException ex) {
            ErrorManager.send(ex);
        }
    }

    public static void run() {
        try {
            LogManager.writeAndPrint("Start running collapsing lite function");

            initInput();
            
            initOutput();

            initGeneSummaryMap();

            Reader in = new FileReader(genotypeFilePath);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withHeader(HEADERS)
                    .withFirstRecordAsHeader()
                    .parse(in);

            String processedVariantID = "";

            for (CSVRecord record : records) {
                String variantID = record.get(VARIANT_ID_HEADER);
                String[] tmp = variantID.split("-");
                String chr = tmp[0];
                int pos = Integer.valueOf(tmp[1]);
                String ref = tmp[2];
                String alt = tmp[3];
                boolean isSnv = ref.length() == alt.length();

                List<String> geneList = getGeneList(record);

                for (String geneName : geneList) {
                    // --gene or --gene-boundary filter applied
                    if (!GeneManager.isValid(geneName, chr, pos)) {
                        continue;
                    }

                    String sampleName = record.get(SAMPLE_NAME_HEADER);
                    Sample sample = SampleManager.getSampleByName(sampleName);

                    if (!summaryMap.containsKey(geneName)) {
                        summaryMap.put(geneName, new CollapsingGeneSummary(geneName));
                    }

                    CollapsingSummary summary = summaryMap.get(geneName);
                    summary.updateSampleVariantCount4SingleVar(sample.getIndex());

                    // only count variant once per gene
                    if (!processedVariantID.equals(variantID)) {
                        summary.updateVariantCount(isSnv);
                    }
                }

                processedVariantID = variantID;
            }

            outputSummary();

            closeOutput();

            if (CollapsingCommand.isMannWhitneyTest) {
                ThirdPartyToolManager.runMannWhitneyTest(genotypeFilePath);
            }

            generatePvaluesQQPlot();

            gzipFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> getGeneList(CSVRecord record) {
        List<String> geneList = new ArrayList();

        String allAnnotation = record.get(ALL_ANNOTATION_HEADER);

        for (String annotation : allAnnotation.split(";")) {
            String geneName = annotation.split("\\|")[1];
            if (!geneList.contains(geneName)) {
                geneList.add(geneName);
            }
        }

        return geneList;
    }

    private static void initGeneSummaryMap() {
        GeneManager.getMap().values().stream().forEach((geneSet) -> {
            geneSet.stream().forEach((gene) -> {
                if (!summaryMap.containsKey(gene.getName())) {
                    summaryMap.put(gene.getName(), new CollapsingGeneSummary(gene.getName()));
                }
            });
        });
    }

    private static void outputSummary() {
        LogManager.writeAndPrint("Output the data to matrix & summary file");

        try {
            summaryList.addAll(summaryMap.values());

            outputMatrix();

            Collections.sort(summaryList);

            int rank = 1;
            for (CollapsingSummary summary : summaryList) {
                bwSummary.write(rank++ + ",");
                bwSummary.write(summary.toString());
                bwSummary.newLine();
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private static void outputMatrix() throws Exception {
        for (CollapsingSummary summary : summaryList) {
            bwSampleMatrix.write(summary.name + "\t");

            for (Sample sample : SampleManager.getList()) {
                bwSampleMatrix.write(summary.variantNumBySample[sample.getIndex()] + "\t");
            }

            bwSampleMatrix.newLine();

            summary.countSample();

            try {
                summary.calculateFetP();
            } catch (Exception e) {
                System.out.println();
            }
        }

        bwSampleMatrix.flush();
        bwSampleMatrix.close();
    }

    private static void generatePvaluesQQPlot() {
        ThirdPartyToolManager.generateQQPlot4CollapsingFetP(summaryFilePath, matrixFilePath, geneFetPQQPlotPath);
    }

    private static void gzipFiles() {
        ThirdPartyToolManager.gzipFile(matrixFilePath);
    }
}
