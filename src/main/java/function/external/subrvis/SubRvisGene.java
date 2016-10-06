package function.external.subrvis;

import java.util.ArrayList;
//
/**
 *
 * @author nick
 */
public class SubRvisGene {

    private String id; // domain or exon identifier
    private String chr;
    private ArrayList<Region> regionList;
    private float score;
    private float oEratio;

    public SubRvisGene(String id,
            String chr,
            ArrayList<Region> regionList,
            float score, 
            float oEratio) {
        this.id = id;
        this.chr = chr;
        this.regionList = regionList;
        this.score = score;
        this.oEratio = oEratio;
    }
    
    public String getId(){
        return id;
    }

    public String getChr() {
        return chr;
    }

    public ArrayList<Region> getRegionList() {
        return regionList;
    }

    public float getScore() {
        return score;
    }
    
    public float getOEratio() {
        return oEratio;
    }

    public boolean isPositionIncluded(String chr, int pos) {
        if (this.chr.equals(chr)) {
            for (Region region : regionList) {
                if (region.isIncluded(pos)) {
                    return true;
                }
            }
        }

        return false;
    }
}
