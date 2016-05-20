package function.coverage.base;

import function.variant.base.Region;
import utils.DBManager;
import utils.ErrorManager;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 *
 * @author qwang
 */
public class Gene {

    String name;
    String boundary;
    String chr;
    ArrayList<Exon> exonList = new ArrayList<Exon>();

    public Gene(String name) {
        this.name = name.trim();

        if (name.contains("(")) {
            boundary = this.name;
            this.name = boundary.substring(0, boundary.indexOf(" "));
        } 

        initChr();
    }

    private void initChr() {
        try {
            chr = "";

            String GENE_CHR = "SELECT name "
                    + "FROM _VAR_TYPE__gene_hit g, _VAR_TYPE_ v, seq_region r "
                    + "WHERE g.gene_name = '_GENE_' "
                    + "AND g._VAR_TYPE__id = v._VAR_TYPE__id "
                    + "AND v.seq_region_id = r.seq_region_id "
                    + "AND coord_system_id = 2 "
                    + "LIMIT 1";

            String geneChrSql = GENE_CHR.replaceAll("_GENE_", name);

            String sql = geneChrSql.replaceAll("_VAR_TYPE_", "snv");

            ResultSet rset = DBManager.executeQuery(sql);

            if (rset.next()) {
                chr = rset.getString("name");
            } else {
                sql = geneChrSql.replaceAll("_VAR_TYPE_", "indel");

                rset = DBManager.executeQuery(sql);

                if (rset.next()) {
                    chr = rset.getString("name");
                }
            }

            rset.close();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    public String getChr() {
        return chr;
    }

    public String getName() {
        return name;
    }

    public boolean contains(Region r) {
        for (Exon exon : exonList) {
            if (exon.contains(r)) {
                return true;
            }
        }

        return false;
    }

    public void initExonList() {
        String[] fields = boundary.trim().replace("(", "").replace(")", "").split(" ");
        name = fields[0];
        chr = fields[1];

        String[] exons = fields[2].trim().split(",");
        for (int i = 0; i < exons.length; i++) {
            int exon_id = i + 1;
            String[] r = exons[i].split("\\W");
            int seq_region_start = Integer.parseInt(r[0]);
            int seq_region_end = Integer.parseInt(r[2]);
            String stable_id = "Exon_" + seq_region_start + "_" + seq_region_end;
            exonList.add(new Exon(exon_id, stable_id, chr, seq_region_start, seq_region_end));
        }
    }

    public ArrayList<Exon> getExonList() {
        return exonList;
    }

    public int getLength() {
        int length = 0;

        for (Exon exon : exonList) {
            length = length + exon.getRegion().getLength();
        }

        return length;
    }

    public String getChrStr() {
        return chr;
    }

    @Override
    public String toString() {
        return name;
    }
}
