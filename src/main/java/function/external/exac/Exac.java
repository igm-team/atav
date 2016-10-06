package function.external.exac;

import global.Data;
import utils.ErrorManager;
import utils.FormatManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.DBManager;

/**
 *
 * @author nick
 */
public class Exac {

    private String chr;
    private int pos;
    private String ref;
    private String alt;
    private boolean isSnv;

    private float meanCoverage;
    private int sampleCovered10x;
    private float[] maf;
    private String[] gts;
    private float vqslod;

    public Exac(String chr, int pos, String ref, String alt) {
        this.chr = chr;
        this.pos = pos;
        this.ref = ref;
        this.alt = alt;

        isSnv = true;

        if (ref.length() > 1
                || alt.length() > 1) {
            isSnv = false;
        }

        initCoverage();

        initMaf();
    }

    public Exac(boolean isIndel, ResultSet rs) {
        try {
            chr = rs.getString("chr");
            pos = rs.getInt("pos");
            ref = rs.getString("ref_allele");
            alt = rs.getString("alt_allele");
            maf = new float[ExacManager.EXAC_POP.length];
            gts = new String[ExacManager.EXAC_POP.length];
            setMaf(rs);

            isSnv = !isIndel;

            initCoverage();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private void initCoverage() {
        try {
            String sql = ExacManager.getSql4Cvg(chr, pos);

            ResultSet rs = DBManager.executeQuery(sql);

            if (rs.next()) {
                meanCoverage = rs.getFloat("mean_cvg");
                sampleCovered10x = rs.getInt("covered_10x");
            } else {
                meanCoverage = Data.NA;
                sampleCovered10x = Data.NA;
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private void initMaf() {
        maf = new float[ExacManager.EXAC_POP.length];
        gts = new String[ExacManager.EXAC_POP.length];

        try {
            String sql = ExacManager.getSql4Maf(isSnv, chr, pos, ref, alt);

            ResultSet rs = DBManager.executeQuery(sql);

            if (rs.next()) {
                setMaf(rs);
            } else if (meanCoverage > 0) {
                resetMaf(0);
            } else {
                resetMaf(Data.NA);
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private void setMaf(ResultSet rs) throws SQLException {
        for (int i = 0; i < ExacManager.EXAC_POP.length; i++) {
            float af = rs.getFloat(ExacManager.EXAC_POP[i] + "_af");

            if (af > 0.5) {
                af = 1 - af;
            }

            maf[i] = af;
            gts[i] = rs.getString(ExacManager.EXAC_POP[i] + "_gts");
        }

        vqslod = rs.getFloat("vqslod");
    }

    private void resetMaf(int value) {
        for (int i = 0; i < ExacManager.EXAC_POP.length; i++) {
            maf[i] = value;
            gts[i] = "NA";
        }

        vqslod = Data.NA;
    }

    private float getMaxMaf() {
        float value = Data.NA;

        for (int i = 0; i < ExacManager.EXAC_POP.length; i++) {
            if (maf[i] != Data.NA
                    && ExacCommand.exacPop.contains(ExacManager.EXAC_POP[i])) {
                value = Math.max(value, maf[i]);
            }
        }

        return value;
    }

    public boolean isValid() {
        return ExacCommand.isExacMafValid(getMaxMaf())
                && ExacCommand.isExacVqslodValid(vqslod, isSnv)
                && ExacCommand.isExacMeanCoverageValid(meanCoverage);
    }

    public String getVariantId() {
        return chr + "-" + pos + "-" + ref + "-" + alt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ExacManager.EXAC_POP.length; i++) {
            sb.append(FormatManager.getDouble(maf[i])).append(",");

            if (gts[i].equals("NA")) {
                sb.append(gts[i]).append(",");
            } else {
                sb.append("'").append(gts[i]).append("',");
            }
        }

        sb.append(FormatManager.getDouble(vqslod)).append(",");
        sb.append(FormatManager.getDouble(meanCoverage)).append(",");
        sb.append(FormatManager.getInteger(sampleCovered10x)).append(",");

        return sb.toString();
    }
}
