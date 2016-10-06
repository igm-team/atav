package function.annotation.base;

import function.external.evs.Evs;
import function.external.evs.EvsCommand;
import function.external.exac.Exac;
import function.external.exac.ExacCommand;
import function.external.exac.ExacManager;
import function.external.genomes.Genomes;
import function.external.genomes.GenomesCommand;
import function.external.gerp.GerpCommand;
import function.external.gerp.GerpManager;
import function.variant.base.Variant;
import function.variant.base.VariantManager;
import function.external.kaviar.Kaviar;
import function.external.kaviar.KaviarCommand;
import function.external.knownvar.KnownVarCommand;
import function.external.knownvar.KnownVarOutput;
import function.external.mgi.MgiCommand;
import function.external.mgi.MgiManager;
import function.external.rvis.RvisCommand;
import function.external.rvis.RvisManager;
import function.external.subrvis.SubRvisCommand;
import function.external.subrvis.SubRvisOutput;
import function.external.trap.TrapCommand;
import function.external.trap.TrapManager;
import function.variant.base.VariantLevelFilterCommand;
import global.Data;
import utils.FormatManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import utils.MathManager;

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
    HashSet<String> geneSet = new HashSet<>();
    HashSet<String> transcriptSet = new HashSet<>();
    double polyphenHumdiv;
    double polyphenHumvar;

    // external db annotations
    Exac exac;
    Kaviar kaviar;
    Evs evs;
    float gerpScore;
    float trapScore;
    KnownVarOutput knownVarOutput;
    private String rvisStr;
    private SubRvisOutput subRvisOutput;
    Genomes genomes;
    private String mgiStr;

    public boolean isValid = true;

    public AnnotatedVariant(int variantId, boolean isIndel, ResultSet rset) throws Exception {
        super(variantId, isIndel, rset);

        if (isIndel) {
            polyphenHumdiv = Data.NA;
            polyphenHumvar = Data.NA;
        } else {
            polyphenHumdiv = MathManager.devide(rset.getInt("polyphen_humdiv"), 1000);
            polyphenHumvar = MathManager.devide(rset.getInt("polyphen_humvar"), 1000);
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
            subRvisOutput = new SubRvisOutput(getGeneName(), getChrStr(), getStartPosition());
        }

        if (MgiCommand.isIncludeMgi) {
            mgiStr = MgiManager.getLine(getGeneName());
        }
    }

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

        if (isValid & ExacCommand.isIncludeExac) {
            exac = new Exac(chrStr, startPosition, refAllele, allele);

            isValid = exac.isValid();
        }

        if (isValid & EvsCommand.isIncludeEvs) {
            evs = new Evs(chrStr, startPosition, refAllele, allele);

            isValid = evs.isValid();
        }

        if (isValid & GerpCommand.isIncludeGerp) {
            gerpScore = GerpManager.getScore(chrStr, startPosition, refAllele, allele);

            isValid = GerpCommand.isGerpScoreValid(gerpScore);
        }

        if (isValid & KaviarCommand.isIncludeKaviar) {
            kaviar = new Kaviar(chrStr, startPosition, refAllele, allele);

            isValid = kaviar.isValid();
        }

        if (isValid & GenomesCommand.isInclude1000Genomes) {
            genomes = new Genomes(chrStr, startPosition, refAllele, allele);

            isValid = genomes.isValid();
        }
    }

    public boolean isValid() {
        return isValid
                & PolyphenManager.isValid(polyphenHumdiv, function, AnnotationLevelFilterCommand.polyphenHumdiv)
                & PolyphenManager.isValid(polyphenHumvar, function, AnnotationLevelFilterCommand.polyphenHumvar)
                & isTrapValid();
    }

    private boolean isTrapValid() {
        if (TrapCommand.isIncludeTrap) {
            if (isIndel()) {
                trapScore = Data.NA;
            } else {
                trapScore = TrapManager.getScore(chrStr, getStartPosition(), allele, geneName);
            }

            if (function.equals("SYNONYMOUS_CODING")
                    || function.equals("INTRON_EXON_BOUNDARY")
                    || function.equals("INTRON")) { 
                // filter only apply to SYNONYMOUS_CODING, INTRON_EXON_BOUNDARY and INTRONIC variants
                return TrapCommand.isTrapScoreValid(trapScore);
            }
        }

        return true;
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
            return exac.toString() + ExacManager.getGeneDamagingCountsLine(geneName);
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

    public String getTrapScore() {
        if (TrapCommand.isIncludeTrap) {
            return FormatManager.getFloat(trapScore) + ",";
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
            return genomes.toString();
        } else {
            return "";
        }
    }

    public String getMgi() {
        if (MgiCommand.isIncludeMgi) {
            return mgiStr;
        } else {
            return "";
        }
    }
}
