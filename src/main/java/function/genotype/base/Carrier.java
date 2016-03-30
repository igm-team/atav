package function.genotype.base;

import function.variant.base.Region;
import global.Data;
import utils.FormatManager;
import java.math.BigDecimal;
import java.sql.ResultSet;

/**
 *
 * @author nick
 */
public class Carrier extends NonCarrier {

    private int gatkFilteredCoverage;
    private int readsRef;
    private int readsAlt;
    private float vqslod;
    private float genotypeQualGQ;
    private float strandBiasFS;
    private float haplotypeScore;
    private float rmsMapQualMQ;
    private float qualByDepthQD;
    private float qual;
    private float readPosRankSum;
    private float mapQualRankSum;
    private String passFailStatus;

    public Carrier(ResultSet rs) throws Exception {
        sampleId = rs.getInt("sample_id");        
        coverage = rs.getInt("samtools_raw_coverage");
        genotype = rs.getInt("genotype");
        gatkFilteredCoverage = rs.getInt("gatk_filtered_coverage");
        readsRef = rs.getInt("reads_ref");
        readsAlt = rs.getInt("reads_alt");
        vqslod = getFloat((Float) rs.getObject("vqslod"));
        genotypeQualGQ = getFloat(rs.getBigDecimal("genotype_qual_GQ"));
        strandBiasFS = getFloat(rs.getBigDecimal("strand_bias_FS"));
        haplotypeScore = getFloat(rs.getBigDecimal("haplotype_score"));
        rmsMapQualMQ = getFloat(rs.getBigDecimal("rms_map_qual_MQ"));
        qualByDepthQD = getFloat(rs.getBigDecimal("qual_by_depth_QD"));
        qual = getFloat(rs.getBigDecimal("qual"));
        readPosRankSum = getFloat(rs.getBigDecimal("read_pos_rank_sum"));
        mapQualRankSum = getFloat(rs.getBigDecimal("map_qual_rank_sum"));
        passFailStatus = rs.getString("pass_fail_status");
    }

    private float getFloat(Float f) {
        if (f == null) {
            return Data.NA;
        }

        return f.floatValue();
    }
    
    private float getFloat(BigDecimal f) {
        if (f == null) {
            return Data.NA;
        }

        return f.floatValue();
    }

    public int getGatkFilteredCoverage() {
        return gatkFilteredCoverage;
    }

    public int getReadsRef() {
        return readsRef;
    }

    public int getReadsAlt() {
        return readsAlt;
    }

    public float getVqslod() {
        return vqslod;
    }

    public float getGenotypeQualGQ() {
        return genotypeQualGQ;
    }

    public float getStrandBiasFS() {
        return strandBiasFS;
    }

    public float getHaplotypeScore() {
        return haplotypeScore;
    }

    public float getRmsMapQualMQ() {
        return rmsMapQualMQ;
    }

    public float getQualByDepthQD() {
        return qualByDepthQD;
    }

    public float getQual() {
        return qual;
    }

    public float getReadPosRankSum() {
        return readPosRankSum;
    }

    public float getMapQualRankSum() {
        return mapQualRankSum;
    }

    public String getPassFailStatus() {
        return passFailStatus;
    }

    private void applyQualityFilter() {
        if (genotype != Data.NA) {
            if (!GenotypeLevelFilterCommand.isVarStatusValid(passFailStatus)
                    || !GenotypeLevelFilterCommand.isGqValid(genotypeQualGQ)
                    || !GenotypeLevelFilterCommand.isFsValid(strandBiasFS)
                    || !GenotypeLevelFilterCommand.isHapScoreValid(haplotypeScore)
                    || !GenotypeLevelFilterCommand.isMqValid(rmsMapQualMQ)
                    || !GenotypeLevelFilterCommand.isQdValid(qualByDepthQD)
                    || !GenotypeLevelFilterCommand.isQualValid(qual)
                    || !GenotypeLevelFilterCommand.isRprsValid(readPosRankSum)
                    || !GenotypeLevelFilterCommand.isMqrsValid(mapQualRankSum)) {
                genotype = Data.NA;
            }
        }

        if (genotype == 2) { // --hom-percent-alt-read 
            double percAltRead = FormatManager.devide(readsAlt, gatkFilteredCoverage);

            if (!GenotypeLevelFilterCommand.isHomPercentAltReadValid(percAltRead)) {
                genotype = Data.NA;
            }
        }
        
        if (genotype == 1) { // --het-percent-alt-read 
            double percAltRead = FormatManager.devide(readsAlt, gatkFilteredCoverage);

            if (!GenotypeLevelFilterCommand.isHetPercentAltReadValid(percAltRead)) {
                genotype = Data.NA;
            }
        }

        if (genotype == Data.NA) {
            coverage = Data.NA;
        }
    }
    
    @Override
    public void applyFilters(Region region){
        super.applyFilters(region);
        
        applyQualityFilter();
    }
}
