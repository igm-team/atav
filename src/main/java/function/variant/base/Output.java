package function.variant.base;

import function.genotype.base.CalledVariant;
import function.genotype.base.GenotypeLevelFilterCommand;
import function.genotype.base.Sample;
import function.genotype.statistics.HWEExact;
import global.Data;
import global.Index;
import function.genotype.base.SampleManager;
import utils.FormatManager;

/**
 *
 * @author nick
 */
public class Output implements Cloneable {

    protected CalledVariant calledVar;
    public int[][] sampleCount = new int[6][3];
    public double[][] sampleFreq = new double[4][3];
    public double varCaseFreq = 0;
    public double varCtrlFreq = 0;
    public double caseMaf = 0;
    public double caseMhgf = 0;
    public double ctrlMaf = 0;
    public double ctrlMhgf = 0;
    public boolean isMinorRef = false;
    // not output
    public int[] totalCov = new int[2];
    public double[] averageCov = new double[2];
    public int varPresent = 0;
    public int caseCarrier = 0;
    public int minorHomCtrl = 0;
    public int minorHomCase = 0;
    public int majorHomCtrl = 0;
    public int majorHomCase = 0;
    public double caseHweP = 0;
    public double ctrlHweP = 0;
    int totalCase;
    int totalCtrl;

    public Output(CalledVariant c) {
        calledVar = c;
    }

    public CalledVariant getCalledVariant() {
        return calledVar;
    }

    public void countSampleGenoCov() {
        int cov, geno, pheno;

        for (Sample sample : SampleManager.getList()) {
            cov = calledVar.getCoverage(sample.getIndex());
            geno = calledVar.getGenotype(sample.getIndex());
            geno = getGenoType(geno, sample);
            pheno = (int) sample.getPheno();

            addSampleGeno(geno, pheno);
            addSampleCov(cov, pheno);
        }
    }

    public void addSampleGeno(int genotype, int pheno) {
        if (genotype == Data.NA) {
            genotype = Index.MISSING;
        }

        sampleCount[genotype][Index.ALL]++;
        sampleCount[genotype][pheno]++;
    }

    public void deleteSampleGeno(int genotype, int pheno) {
        if (genotype == Data.NA) {
            genotype = Index.MISSING;
        }

        sampleCount[genotype][Index.ALL]--;
        sampleCount[genotype][pheno]--;
    }

    public void addSampleCov(int cov, int phone) {
        if (cov != Data.NA) {
            totalCov[phone] += cov;
        }
    }

    public void deleteSampleCov(int cov, int phone) {
        if (cov != Data.NA) {
            totalCov[phone] -= cov;
        }
    }

    public void countMissingSamples() {
        sampleCount[Index.MISSING][Index.CASE] = SampleManager.getCaseNum();
        sampleCount[Index.MISSING][Index.CTRL] = SampleManager.getCtrlNum();

        for (int i = 0; i < sampleCount.length - 1; i++) {
            sampleCount[Index.MISSING][Index.CASE] -= sampleCount[i][Index.CASE];
            sampleCount[Index.MISSING][Index.CTRL] -= sampleCount[i][Index.CTRL];
        }

        sampleCount[Index.MISSING][Index.ALL] = sampleCount[Index.MISSING][Index.CTRL]
                + sampleCount[Index.MISSING][Index.CASE];
    }

    public void calculate() {
        calculateTotalCaseAndCtrl();

        calculateSampleFreq();

        calculateHweP(); // only collapsing, fisher, linear, var geno, family output

        calculateAvgCov();

        calculateVarFreq();

        checkMinorRef();

        calculateMaf();

        calculateMhgf();

        countVarPresentAndCaseCarrier();
    }

    private void calculateTotalCaseAndCtrl() {
        totalCase = sampleCount[Index.HOM][Index.CASE]
                + sampleCount[Index.HET][Index.CASE]
                + sampleCount[Index.REF][Index.CASE]
                + sampleCount[Index.HOM_MALE][Index.CASE]
                + sampleCount[Index.REF_MALE][Index.CASE];

        totalCtrl = sampleCount[Index.HOM][Index.CTRL]
                + sampleCount[Index.HET][Index.CTRL]
                + sampleCount[Index.REF][Index.CTRL]
                + sampleCount[Index.HOM_MALE][Index.CTRL]
                + sampleCount[Index.REF_MALE][Index.CTRL];
    }

    private void calculateSampleFreq() {
        sampleFreq[Index.HOM][Index.CASE]
                = FormatManager.devide(sampleCount[Index.HOM][Index.CASE]
                        + sampleCount[Index.HOM_MALE][Index.CASE], totalCase);

        sampleFreq[Index.HET][Index.CASE]
                = FormatManager.devide(sampleCount[Index.HET][Index.CASE], totalCase);

        sampleFreq[Index.HOM][Index.CTRL]
                = FormatManager.devide(sampleCount[Index.HOM][Index.CTRL]
                        + sampleCount[Index.HOM_MALE][Index.CTRL], totalCtrl);

        sampleFreq[Index.HET][Index.CTRL]
                = FormatManager.devide(sampleCount[Index.HET][Index.CTRL], totalCtrl);
    }

    public void calculateHweP() {
        caseHweP = HWEExact.getP(sampleCount[Index.HOM][Index.CASE],
                sampleCount[Index.HET][Index.CASE],
                sampleCount[Index.REF][Index.CASE]);

        ctrlHweP = HWEExact.getP(sampleCount[Index.HOM][Index.CTRL],
                sampleCount[Index.HET][Index.CTRL],
                sampleCount[Index.REF][Index.CTRL]);
    }

    private void calculateAvgCov() {
        averageCov[Index.CASE] = FormatManager.devide(totalCov[Index.CASE], totalCase);
        averageCov[Index.CTRL] = FormatManager.devide(totalCov[Index.CTRL], totalCtrl);
    }

    private void calculateVarFreq() {
        int varCase = 2 * sampleCount[Index.HOM][Index.CASE]
                + sampleCount[Index.HET][Index.CASE]
                + sampleCount[Index.HOM_MALE][Index.CASE];
        int caseNum = varCase + sampleCount[Index.HET][Index.CASE]
                + 2 * sampleCount[Index.REF][Index.CASE]
                + sampleCount[Index.REF_MALE][Index.CASE];

        varCaseFreq = FormatManager.devide(varCase, caseNum); // (2*hom + het + homMale) / (2*hom + homMale +2*het+2*ref + refMale)

        int varCtrl = 2 * sampleCount[Index.HOM][Index.CTRL]
                + sampleCount[Index.HET][Index.CTRL]
                + sampleCount[Index.HOM_MALE][Index.CTRL];
        int ctrlNum = varCtrl + sampleCount[Index.HET][Index.CTRL]
                + 2 * sampleCount[Index.REF][Index.CTRL]
                + sampleCount[Index.REF_MALE][Index.CTRL];

        varCtrlFreq = FormatManager.devide(varCtrl, ctrlNum); // (2*hom + het + homMale) / (2*hom + homMale +2*het+2*ref + refMale)
    }

    public void checkMinorRef() {
        if (varCtrlFreq > 0.5) {
            isMinorRef = true;
        } else {
            isMinorRef = false;
        }
    }

    private void calculateMaf() {
        caseMaf = varCaseFreq;
        ctrlMaf = varCtrlFreq;

        if (isMinorRef) {
            ctrlMaf = 1.0 - varCtrlFreq; // (2*ref + het + refMale) / (2*hom + homMale +2*het+2*ref + refMale)

            if (varCaseFreq != Data.NA) {
                caseMaf = 1.0 - varCaseFreq;
            }
        }
    }

    private void calculateMhgf() {
        caseMhgf = sampleFreq[Index.HOM][Index.CASE];
        ctrlMhgf = sampleFreq[Index.HOM][Index.CTRL]; // hom / (hom + het + ref)

        if (isMinorRef) {
            ctrlMhgf = FormatManager.devide(sampleCount[Index.REF][Index.CTRL]
                    + sampleCount[Index.REF_MALE][Index.CTRL],
                    totalCtrl); // ref / (hom + het + ref)

            caseMhgf = FormatManager.devide(sampleCount[Index.REF][Index.CASE]
                    + sampleCount[Index.REF_MALE][Index.CASE],
                    totalCase); // ref / (hom + het + ref)
        }
    }

    public void countVarPresentAndCaseCarrier() {
        int refCase = sampleCount[Index.REF][Index.CASE]
                + sampleCount[Index.REF_MALE][Index.CASE];

        int homCase = sampleCount[Index.HOM][Index.CASE]
                + sampleCount[Index.HOM_MALE][Index.CASE];

        int refCtrl = sampleCount[Index.REF][Index.CTRL]
                + sampleCount[Index.REF_MALE][Index.CTRL];

        int homCtrl = sampleCount[Index.HOM][Index.CTRL]
                + sampleCount[Index.HOM_MALE][Index.CTRL];

        if (isMinorRef) {
            minorHomCase = refCase;
            majorHomCase = homCase;
            minorHomCtrl = refCtrl;
            majorHomCtrl = homCtrl;
        } else {
            minorHomCase = homCase;
            majorHomCase = refCase;
            minorHomCtrl = homCtrl;
            majorHomCtrl = refCtrl;
        }

        caseCarrier = minorHomCase + sampleCount[Index.HET][Index.CASE];

        varPresent = caseCarrier + minorHomCtrl
                + sampleCount[Index.HET][Index.CTRL];
    }

    public void reset() {
        sampleCount = new int[6][3];
        sampleFreq = new double[4][3];
        totalCov = new int[2];
        averageCov = new double[2];
    }

    public int getGenoType(int geno, Sample sample) {
        if (sample.isMale()
                && !calledVar.getRegion().isInsideAutosomalOrPseudoautosomalRegions()) {

            if (geno == Index.HOM) {
                return Index.HOM_MALE;
            } else if (geno == Index.REF) {
                return Index.REF_MALE;
            }

        }

        return geno;
    }

    public String getGenoStr(int geno) {
        switch (geno) {
            case 2:
                return "hom";
            case 1:
                return "het";
            case 0:
                return "hom ref";
            case Data.NA:
                return "NA";
        }

        return "";
    }

    public boolean isValid() {
        if (GenotypeLevelFilterCommand.isMinVarPresentValid(varPresent)
                && GenotypeLevelFilterCommand.isMinCaseCarrierValid(caseCarrier)
                && GenotypeLevelFilterCommand.isMinCtrlAverageCoverageValid(averageCov[Index.CTRL])
                && GenotypeLevelFilterCommand.isMinCtrlMafValid(ctrlMaf)) {
            boolean isRecessive = isRecessive();

            if (isMaxCtrlMafValid(isRecessive)
                    && isMaxCtrlMhgfValid(isRecessive)) {
                return true;
            }
        }

        return false;
    }

    /*
     * if ref is minor then only het & ref are qualified samples. If ref is
     * major then only hom & het are qualified samples.
     */
    public boolean isQualifiedGeno(int geno) {
        if (GenotypeLevelFilterCommand.isAllGeno) {
            return true;
        }

        if (GenotypeLevelFilterCommand.isAllNonRef) {
            if (geno == 2 || geno == 1) {
                return true;
            }
        }

        if (isMinorRef) {
            if (geno == 0 || geno == 1) {
                return true;
            }
        } else {
            if (geno == 2 || geno == 1) {
                return true;
            }
        }

        return false;
    }

    public boolean isRecessive() {
        if (isMinorRef) {
            if (sampleCount[Index.REF][Index.ALL]
                    + sampleCount[Index.REF_MALE][Index.ALL] > 0) {
                return true;
            }
        } else {
            if (sampleCount[Index.HOM][Index.ALL]
                    + sampleCount[Index.HOM_MALE][Index.ALL] > 0) {
                return true;
            }
        }

        return false;
    }

    public boolean isMaxCtrlMafValid(boolean isRecessive) {
        if (isRecessive) {
            return GenotypeLevelFilterCommand.isMaxCtrlMafRecValid(ctrlMaf);
        } else {
            return GenotypeLevelFilterCommand.isMaxCtrlMafValid(ctrlMaf);
        }
    }

    public boolean isMaxCtrlMhgfValid(boolean isRecessive) {
        if (isRecessive) {
            return GenotypeLevelFilterCommand.isMaxCtrlMhgfRecValid(ctrlMhgf);
        } else {
            return true;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Output output = (Output) super.clone();

        output.sampleCount = FormatManager.deepCopyIntArray(sampleCount);
        output.sampleFreq = FormatManager.deepCopyDoubleArray(sampleFreq);
        output.totalCov = FormatManager.deepCopyIntArray(totalCov);
        output.averageCov = FormatManager.deepCopyDoubleArray(averageCov);

        return output;
    }
}
