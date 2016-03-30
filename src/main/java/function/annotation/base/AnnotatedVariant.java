package function.annotation.base;

import function.external.evs.Evs;
import function.external.evs.EvsCommand;
import function.external.exac.Exac;
import function.external.exac.ExacCommand;
import function.external.genomes.GenomesCommand;
import function.external.genomes.GenomesOutput;
import function.external.gerp.GerpCommand;
import function.external.gerp.GerpManager;
import function.variant.base.Variant;
import function.variant.base.VariantManager;
import function.external.kaviar.Kaviar;
import function.external.kaviar.KaviarCommand;
import function.external.knownvar.KnownVarCommand;
import function.external.knownvar.KnownVarOutput;
import function.external.rvis.RvisCommand;
import function.external.rvis.RvisManager;
import function.external.subrvis.SubRvisCommand;
import function.external.subrvis.SubRvisOutput;
import function.variant.base.VariantLevelFilterCommand;
import global.Data;
import utils.FormatManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author nick
 */
public class AnnotatedVariant extends Variant {

    // AnnoDB annotations
    String function;
    String geneName;
    String codonChange;
    String aminoAcidChange;
    String stableId;
    HashSet<String> geneSet = new HashSet<String>();
    HashSet<String> transcriptSet = new HashSet<String>();
    double polyphenHumdiv;
    double polyphenHumvar;

    // external db annotations
    Exac exac;
    Kaviar kaviar;
    Evs evs;
    float gerpScore;
    KnownVarOutput knownVarOutput;
    private String rvisStr;
    private SubRvisOutput subRvisOutput;
    GenomesOutput genomesOutput;

    public boolean isValid = true;

    public AnnotatedVariant(int variantId, boolean isIndel, ResultSet rset) throws Exception {
        super(variantId, isIndel, rset);

        if (isIndel) {
            polyphenHumdiv = Data.NA;
            polyphenHumvar = Data.NA;
        } else {
            polyphenHumdiv = FormatManager.devide(rset.getInt("polyphen_humdiv"), 1000);
            polyphenHumvar = FormatManager.devide(rset.getInt("polyphen_humvar"), 1000);
        }

        function = "";
        geneName = "";
        codonChange = "";
        aminoAcidChange = "";
        stableId = "";

        checkValid();
    }

    public void initExternalData() {
        if (KnownVarCommand.isIncludeKnownVar) {
            knownVarOutput = new KnownVarOutput(this);
        }

        if (RvisCommand.isIncludeRvis) {
            rvisStr = RvisManager.getLine(getGeneName());
        }

        if (SubRvisCommand.isIncludeSubRvis) {
            subRvisOutput = new SubRvisOutput(getGeneName(),
                    getRegion().getChrStr(),
                    getRegion().getStartPosition());
        }
    }

    // update code below for unit testing
//    
//    public AnnotatedVariant(int v_id, boolean isIndel,
//            String alt, String ref, String rs,
//            int pos, String chr) throws Exception {
//        super(v_id, isIndel, alt, ref, rs, pos, chr);
//    }
//    
//    public static void main(String[] args) throws Exception {
//        System.out.println("test");
//
//        AnnotatedVariant variant = new AnnotatedVariant(0, false, "C", "T", "", 78082311, "17");
//
//        variant.aminoAcidChange = "N570K";
//        variant.codonChange = "aaC/aaG";
//
//        String result = variant.getCodingSequenceChange();
//
//        System.out.println(result);
//    }
    public void update(Annotation annotation) {
        if (isValid) {
            geneSet.add(annotation.geneName);

            if (function.isEmpty()
                    || FunctionManager.isMoreDamage(annotation.function, function)) {
                function = annotation.function;
                geneName = annotation.geneName;
                codonChange = annotation.codonChange;
                aminoAcidChange = annotation.aminoAcidChange;
                stableId = annotation.stableId;
            }

            transcriptSet.add(annotation.function + "|"
                    + annotation.geneName + "|"
                    + annotation.stableId
                    + "(" + annotation.aminoAcidChange + ")");

            if (polyphenHumdiv < annotation.polyphenHumdiv) {
                polyphenHumdiv = annotation.polyphenHumdiv;
            }

            if (polyphenHumvar < annotation.polyphenHumvar) {
                polyphenHumvar = annotation.polyphenHumvar;
            }
        }
    }

    private void checkValid() throws Exception {
        isValid = VariantLevelFilterCommand.isCscoreValid(cscorePhred);

        if (isValid) {
            isValid = VariantManager.isValid(this);
        }

        if (isValid & GerpCommand.isIncludeGerp) {
            gerpScore = GerpManager.getScore(variantIdStr);

            isValid = GerpCommand.isGerpScoreValid(gerpScore);
        }

        if (isValid & ExacCommand.isIncludeExac) {
            exac = new Exac(variantIdStr);

            isValid = exac.isValid();
        }

        if (isValid & KaviarCommand.isIncludeKaviar) {
            kaviar = new Kaviar(variantIdStr);

            isValid = kaviar.isValid();
        }

        if (isValid & GenomesCommand.isInclude1000Genomes) {
            genomesOutput = new GenomesOutput(variantIdStr);
            
            isValid = genomesOutput.isValid();
        }

        if (isValid & EvsCommand.isIncludeEvs) {
            evs = new Evs(variantIdStr);

            isValid = evs.isValid();
        }
    }

    public boolean isValid() {
        return isValid
                & PolyphenManager.isValid(polyphenHumdiv, function, AnnotationLevelFilterCommand.polyphenHumdiv)
                & PolyphenManager.isValid(polyphenHumvar, function, AnnotationLevelFilterCommand.polyphenHumvar);
    }

    public String getGeneName() {
        if (geneName.isEmpty()) {
            return "NA";
        }

        return geneName;
    }

    public String getFunction() {
        return function;
    }

    public String getCodonChange() {
        if (codonChange.isEmpty()) {
            return "NA";
        }

        return codonChange;
    }

    public String getStableId() {
        if (stableId.isEmpty()) {
            return "NA";
        }

        return stableId;
    }

    public String getAminoAcidChange() {
        if (aminoAcidChange.isEmpty()) {
            return "NA";
        }

        return aminoAcidChange;
    }

    public String getCodingSequenceChange() {
        if (aminoAcidChange.isEmpty()
                || aminoAcidChange.equals("NA")
                || isIndel()) {
            return "NA";
        }

        String posStr = "";

        for (int i = 0; i < aminoAcidChange.length(); i++) {
            char c = aminoAcidChange.charAt(i);

            if (Character.isDigit(c)) {
                posStr += c;
            }
        }

        int aminoAcidPos = Integer.valueOf(posStr);

        String leftStr = codonChange.split("/")[0];
        String rightStr = codonChange.split("/")[1];

        int codingPos = Data.NA;
        int changeIndex = Data.NA;
        int[] codonOffBase = {2, 1, 0}; // aminoAcidPos * 3 is the last position of codon

        for (int i = 0; i < leftStr.length(); i++) {
            if (leftStr.charAt(i) != rightStr.charAt(i)) {
                codingPos = aminoAcidPos * 3 - codonOffBase[i];
                changeIndex = i;
            }
        }

        return "c." + codingPos + leftStr.charAt(changeIndex)
                + ">" + rightStr.charAt(changeIndex);
    }

    public String getTranscriptSet() {
        if (transcriptSet.size() > 0) {
            Set set = new TreeSet(transcriptSet);
            return set.toString().replaceAll(", ", ";").replace("[", "").replace("]", "");
        }

        return "NA";
    }

    public HashSet<String> getGeneSet() {
        return geneSet;
    }

    public String getPolyphenHumdivScore() {
        if (!function.startsWith("NON_SYNONYMOUS")
                || polyphenHumdiv < 0) {
            polyphenHumdiv = Data.NA;
        }

        return FormatManager.getDouble(polyphenHumdiv);
    }

    public String getPolyphenHumvarScore() {
        if (!function.startsWith("NON_SYNONYMOUS")
                || polyphenHumvar < 0) {
            polyphenHumvar = Data.NA;
        }

        return FormatManager.getDouble(polyphenHumvar);
    }

    public String getPolyphenHumdivPrediction() {
        return getPredictionByScore(polyphenHumdiv);
    }

    public String getPolyphenHumvarPrediction() {
        return getPredictionByScore(polyphenHumvar);
    }

    private String getPredictionByScore(double score) {
        String prediction = PolyphenManager.getPrediction(score, function);

        prediction = prediction.replaceAll("probably", "probably_damaging");
        prediction = prediction.replaceAll("possibly", "possibly_damaging");

        return prediction;
    }

    public String getExacStr() {
        if (ExacCommand.isIncludeExac) {
            return exac.toString();
        } else {
            return "";
        }
    }

    public String getKaviarStr() {
        if (KaviarCommand.isIncludeKaviar) {
            return kaviar.toString();
        } else {
            return "";
        }
    }

    public String getEvsStr() {
        if (EvsCommand.isIncludeEvs) {
            return evs.toString();
        } else {
            return "";
        }
    }

    public String getKnownVarStr() {
        if (KnownVarCommand.isIncludeKnownVar) {
            return knownVarOutput.toString();
        } else {
            return "";
        }
    }

    public String getGerpScore() {
        if (GerpCommand.isIncludeGerp) {
            return FormatManager.getFloat(gerpScore) + ",";
        } else {
            return "";
        }
    }

    public String getRvis() {
        if (RvisCommand.isIncludeRvis) {
            return rvisStr;
        } else {
            return "";
        }
    }

    public String getSubRvis() {
        if (SubRvisCommand.isIncludeSubRvis) {
            return subRvisOutput.toString();
        } else {
            return "";
        }
    }

    public String get1000Genomes() {
        if (GenomesCommand.isInclude1000Genomes) {
            return genomesOutput.toString();
        } else {
            return "";
        }
    }
}
