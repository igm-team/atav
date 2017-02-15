package function.genotype.collapsing;

import function.genotype.base.CalledVariant;
import function.genotype.base.Sample;
import function.genotype.base.SampleManager;
import global.Index;
import utils.CommonCommand;
import utils.ErrorManager;
import utils.FormatManager;
import utils.LogManager;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import utils.MathManager;

/**
 *
 * @author nick
 */
public class CollapsingCompHet extends CollapsingBase {

    BufferedWriter bwCompHet = null;
    final String comphetFilePath = CommonCommand.outputPath + "comphet.csv";

    HashSet<Integer> variantIdSet = new HashSet<>();
    HashMap<String, List<CompHetOutput>> geneVariantListMap = new HashMap<>();

    @Override
    public void initOutput() {
        try {
            super.initOutput();

            bwCompHet = new BufferedWriter(new FileWriter(comphetFilePath));
            bwCompHet.write(CompHetOutput.getTitle());
            bwCompHet.newLine();
        } catch (Exception ex) {
            ErrorManager.send(ex);
        }
    }

    @Override
    public void doOutput() {
        listCompHets();

        clearList();
    }

    @Override
    public void closeOutput() {
        try {
            super.closeOutput();

            bwCompHet.flush();
            bwCompHet.close();
        } catch (Exception ex) {
            ErrorManager.send(ex);
        }
    }

    @Override
    public void processVariant(CalledVariant calledVar) {
        try {
            CompHetOutput output = new CompHetOutput(calledVar);

            addVariantToGeneList(output);
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private void addVariantToGeneList(CompHetOutput output) {
        List<CompHetOutput> geneOutputList
                = geneVariantListMap.get(output.getCalledVariant().getGeneName());

        if (geneOutputList == null) {
            geneOutputList = new ArrayList<>();
            geneOutputList.add(output);
            geneVariantListMap.put(output.getCalledVariant().getGeneName(), geneOutputList);
        } else {
            geneOutputList.add(output);
        }
    }

    private void listCompHets() {
        if (geneVariantListMap.isEmpty()) {
            return;
        }

        try {
            for (Entry<String, List<CompHetOutput>> entry : geneVariantListMap.entrySet()) {
                LogManager.writeAndPrint("Processing variants in gene:" + entry.getKey());

                CollapsingSummary summary = summaryMap.get(entry.getKey());

                if (summary == null) {
                    summary = new CollapsingGeneSummary(entry.getKey());
                    summaryMap.put(entry.getKey(), summary);
                }

                doOutput(entry.getValue(), summary);
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private void doOutput(List<CompHetOutput> geneOutputList, CollapsingSummary summary) {
        try {
            int outputSize = geneOutputList.size();

            CompHetOutput output1, output2;

            for (Sample sample : SampleManager.getList()) {

                for (int i = 0; i < outputSize; i++) {

                    output1 = geneOutputList.get(i);

                    byte geno1 = output1.getCalledVariant().getGT(sample.getIndex());

                    if (output1.isQualifiedGeno(geno1)) {

                        output1.calculateLooFreq(sample);

                        if (output1.isMaxLooMafValid()) {

                            if (isOutputValid(output1, geno1, sample, summary)) {
                                continue;
                            }

                            for (int j = i + 1; j < outputSize; j++) {

                                output2 = geneOutputList.get(j);

                                checkOutputValid(output1, output2, sample, summary);
                            }
                        }
                    }
                }
            }

            variantIdSet.clear();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private boolean isOutputValid(CompHetOutput output1, byte geno,
            Sample sample, CollapsingSummary summary) throws Exception {
        if (output1.isHomOrRef(geno)) {
            summary.updateSampleVariantCount4CompHet(sample.getIndex());

            updateSummaryVariantCount(output1, summary);

            StringBuilder sb = new StringBuilder();
            sb.append(sample.getFamilyId()).append(",");
            sb.append("NA,"); // Var Case Freq #1 & #2 (co-occurance)
            sb.append("NA,"); // Var Ctrl Freq #1 & #2 (co-occurance)
            sb.append(output1.getString(sample));

            bwCompHet.write(sb.toString());
            bwCompHet.newLine();

            return true;
        }

        return false;
    }

    private void checkOutputValid(CompHetOutput output1, CompHetOutput output2,
            Sample sample, CollapsingSummary summary) throws Exception {
        if (output1.getCalledVariant().getVariantIdNegative4Indel()
                != output2.getCalledVariant().getVariantIdNegative4Indel()) {

            byte geno2 = output2.getCalledVariant().getGT(sample.getIndex());

            if (output2.isQualifiedGeno(geno2)) {

                output2.calculateLooFreq(sample);

                if (output2.isMaxLooMafValid()) {

                    float[] coFreq = getCoOccurrenceFreq(output1, output2);

                    summary.updateSampleVariantCount4CompHet(sample.getIndex());

                    updateSummaryVariantCount(output1, summary);
                    updateSummaryVariantCount(output2, summary);

                    StringBuilder sb = new StringBuilder();
                    sb.append(sample.getFamilyId()).append(",");
                    sb.append(FormatManager.getFloat(coFreq[Index.CASE])).append(",");
                    sb.append(FormatManager.getFloat(coFreq[Index.CTRL])).append(",");
                    sb.append(output1.getString(sample));
                    sb.append(output2.getString(sample));

                    bwCompHet.write(sb.toString());
                    bwCompHet.newLine();
                }
            }
        }
    }

    /*
     * The number of people who have BOTH of the variants divided by the total
     * number of covered people. freq[0] Frequency of Variant #1 & #2
     * (co-occurance) in cases. freq[1] Frequency of Variant #1 & #2
     * (co-occurance) in ctrls
     */
    private float[] getCoOccurrenceFreq(CompHetOutput output1, CompHetOutput output2) {
        float[] freq = new float[2];

        int quanlifiedCaseCount = 0, qualifiedCtrlCount = 0;
        int totalCaseCount = 0, totalCtrlCount = 0;

        for (Sample sample : SampleManager.getList()) {
            boolean isCoQualifiedGeno = isCoQualifiedGeno(output1, output2, sample.getIndex());

            if (sample.isCase()) {
                totalCaseCount++;
                if (isCoQualifiedGeno) {
                    quanlifiedCaseCount++;
                }
            } else {
                totalCtrlCount++;
                if (isCoQualifiedGeno) {
                    qualifiedCtrlCount++;
                }
            }
        }

        freq[Index.CTRL] = MathManager.devide(qualifiedCtrlCount, totalCtrlCount);
        freq[Index.CASE] = MathManager.devide(quanlifiedCaseCount, totalCaseCount);

        return freq;
    }

    private boolean isCoQualifiedGeno(CompHetOutput output1,
            CompHetOutput output2, int index) {
        byte geno1 = output1.getCalledVariant().getGT(index);
        byte geno2 = output2.getCalledVariant().getGT(index);

        return output1.isQualifiedGeno(geno1)
                && output2.isQualifiedGeno(geno2);
    }

    private void updateSummaryVariantCount(CompHetOutput output, CollapsingSummary summary) {
        if (!variantIdSet.contains(output.getCalledVariant().getVariantIdNegative4Indel())) {
            summary.updateVariantCount(output);
            variantIdSet.add(output.getCalledVariant().getVariantIdNegative4Indel());
        }
    }

    private void clearList() {
        geneVariantListMap.clear();
    }

    @Override
    public String toString() {
        return "Start running collapsing compound heterozygosity function";
    }
}
