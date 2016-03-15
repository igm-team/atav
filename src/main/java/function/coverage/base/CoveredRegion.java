package function.coverage.base;

import function.variant.base.Region;
import global.Data;
import function.genotype.base.GenotypeLevelFilterCommand;
import function.genotype.base.SampleManager;
import utils.DBManager;
import utils.ErrorManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author qwang
 */
public class CoveredRegion extends Region {

    public CoveredRegion(int id, String chr, int start, int end) {
        super(id, chr, start, end);
    }

    public CoveredRegion(String chr, int start, int end) {
        super(chr, start, end);
    }

    public ArrayList<HashMap<Integer, Integer>> getCoverage(int[] min_cov) {
        String strQuery = getCoverageString(0, min_cov[0]); //for genome
        ArrayList<HashMap<Integer, Integer>> result = CalculateCoverage(strQuery, min_cov);
        strQuery = getCoverageString(1, min_cov[0]); //for exome
        ArrayList<HashMap<Integer, Integer>> result1 = CalculateCoverage(strQuery, min_cov);
        strQuery = getEVSCoverageRangeString();

        ArrayList<HashMap<Integer, Integer>> result2 = null;

        for (int i = 0; i < min_cov.length; i++) { //merge genome and exome results
            result.get(i).putAll(result1.get(i));
        }
        return result;
    }

    public ArrayList<int[]> getCoverageForSites(int min_cov) {
        String strQuery = getCoverageString(0, min_cov); //for genome
        ArrayList<int[]> result = CalculateCoverageForSites(strQuery);
        strQuery = getCoverageString(1, min_cov); //for exome
        ArrayList<int[]> result1 = CalculateCoverageForSites(strQuery);
        for (int i = 0; i < result.get(0).length; i++) {
            result.get(0)[i] += result1.get(0)[i]; //case
            result.get(1)[i] += result1.get(1)[i]; //control
        }
        return result;
    }

    public String getEVSCoverageRangeString() {
        if (chrStr.length() > 2) { //a quick and dirty check
            return "";
        } else {
            String str = SqlQuery.EVS_COVERAGE_RANGE;
            str = str.replaceAll("_CHR_", chrStr);
            str = str.replaceAll("_POS1_", new Integer(startPosition).toString());
            str = str.replaceAll("_POS2_", new Integer(endPosition).toString());
            return str;
        }

    }

    public String getCoverageString(int DataTypeIndex, int min_cov) {//min_cov  not used here
        if (chrStr.length() > 2) {
            return "";
        } else {
            String str = SqlQuery.Region_Coverage_1024;
            str = str.replaceAll("_SAMPLE_TYPE_", Data.SAMPLE_TYPE[DataTypeIndex]);
            str = str.replaceAll("_CHROM_", chrStr);
            str = str.replaceAll("_POSITIONS_", getPositionString());
            return str;
        }
    }

    public boolean contains(Region r) {
        return r.getChrStr().equalsIgnoreCase(chrStr)
                && r.getStartPosition() >= startPosition
                && r.getStartPosition() <= endPosition;
    }

    public CoveredRegion intersect(int start, int end) {
        if (end >= startPosition && start <= endPosition) {
            int newstart = Math.max(startPosition, start);
            int newend = Math.min(endPosition, end);
            return new CoveredRegion(regionId, chrStr, newstart, newend);
        }
        return null;
    }

    // find the intersection of two regions
    public CoveredRegion intersect(Region other) {
        if (regionId == other.getRegionId()) {
            if (other.getEndPosition() >= startPosition && other.getStartPosition() <= endPosition) {
                int start = Math.max(startPosition, other.getStartPosition());
                int end = Math.min(endPosition, other.getEndPosition());
                return new CoveredRegion(regionId, chrStr, start, end);
            }
        }

        return null;
    }

    public int intersectLength(int region_start, int region_end) {
        if (region_end >= startPosition && region_start <= endPosition) {
            int start = Math.max(startPosition, region_start);
            int end = Math.min(endPosition, region_end);
            return end - start + 1;
        } else {
            return 0;
        }
    }

    public ArrayList<HashMap<Integer, Integer>> CalculateCoverage(String strQuery, int[] min_cov) {
        ArrayList<HashMap<Integer, Integer>> result = new ArrayList<HashMap<Integer, Integer>>();
        for (int i = 0; i < min_cov.length; i++) {
            result.add(new HashMap<Integer, Integer>());
        }
        if (!strQuery.isEmpty()) {
            try {
                ResultSet rs = DBManager.executeQuery(strQuery);
                while (rs.next()) {
                    String strCoverage = rs.getString("min_coverage");
                    int position = rs.getInt("position");
                    ArrayList<CoverageInterval> cilist = getCoverageIntervalListByMinCoverage(position,
                            strCoverage, min_cov[0], false);
                    for (CoverageInterval ci : cilist) {
                        int overlap = intersectLength(ci.getStartPos(), ci.getEndPos());
                        if (overlap > 0) {
                            int sample_id = rs.getInt("sample_id");
                            int min_coverage = ci.getCoverage();
                            for (int i = 0; i < min_cov.length; i++) {
                                if (min_coverage >= min_cov[i]) {
                                    HashMap<Integer, Integer> currentMap = result.get(i);
                                    if (currentMap.containsKey(sample_id)) {
                                        currentMap.put(sample_id, currentMap.get(sample_id) + overlap);
                                    } else {
                                        currentMap.put(sample_id, overlap);
                                    }
                                }
                            }
                        }
                    }

                }
                rs.close();
            } catch (Exception e) {
                ErrorManager.send(e);
            }
        }
        return result;
    }

    public ArrayList<int[]> CalculateCoverageForSites(String strQuery) {
        int SiteStart = getStartPosition();
        // now store results for cases and controls separately
        ArrayList<int []> result = new ArrayList<>();
        result.add(new int[getLength()]);
        result.add(new int[getLength()]);
       
        int min_coverage = GenotypeLevelFilterCommand.minCoverage;
        if (!strQuery.isEmpty()) {
            try {
                ResultSet rs = DBManager.executeQuery(strQuery);
                while (rs.next()) {
                    String strCoverage = rs.getString("min_coverage");
                    int position = rs.getInt("position");
                    int sampleid = rs.getInt("sample_id");
                    boolean isCase = SampleManager.getMap().get(sampleid).isCase();
                    ArrayList<CoverageInterval> cilist = getCoverageIntervalListByMinCoverage(position,
                            strCoverage, min_coverage, false);
                    for (CoverageInterval ci : cilist) {
                        CoveredRegion cr = intersect(ci.getStartPos(), ci.getEndPos());
                        if (cr != null) {
                            for (int i = cr.getStartPosition(); i <= cr.getEndPosition(); i++) {
                                if (isCase) {
                                    result.get(0)[i - SiteStart]++;
                                } else {
                                    result.get(1)[i - SiteStart]++;
                                }
                            }
                        }
                    }
                }
                rs.close();

            } catch (Exception e) {
                ErrorManager.send(e);
            }
        }
        return result;
    }

    private int getPosition(int pos) { //optimize it later
        int posIndex = pos % Data.COVERAGE_BLOCK_SIZE; // coverage data block size is 1024
        if (posIndex == 0) {
            posIndex = Data.COVERAGE_BLOCK_SIZE; // block boundary is ( ] 
        }
        return pos - posIndex + Data.COVERAGE_BLOCK_SIZE;
    }

    private String getPositionString() {
        int firstIndex = getPosition(startPosition);
        int lastIndex = getPosition(endPosition);
        if (firstIndex == lastIndex) {
            return Integer.toString(firstIndex);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int index = firstIndex; index < lastIndex; index += Data.COVERAGE_BLOCK_SIZE) {
                sb.append(Integer.toString(index)).append(",");
            }
            sb.append(Integer.toString(lastIndex));
            return sb.toString();
        }
    }

    private static ArrayList<CoverageInterval> getCoverageIntervalListByMinCoverage(
            int sampleBlockPos, String sampleCovBinStr, int minCoverage, boolean isSortingByCov) {
        String[] allCovBinArray = sampleCovBinStr.split(",");

        ArrayList<CoverageInterval> list = new ArrayList<CoverageInterval>();

        int endIndex = 0;

        String oneCovBinStr, oneCovBinLength, covStr;

        for (int i = 0; i < allCovBinArray.length; i++) {
            oneCovBinStr = allCovBinArray[i];
            oneCovBinLength = oneCovBinStr.substring(0, oneCovBinStr.length() - 1);

            int startIndex = endIndex + 1;
            endIndex += Integer.valueOf(oneCovBinLength);

            covStr = oneCovBinStr.substring(oneCovBinStr.length() - 1);
            int cov = getCoverageByBin(covStr);

            if (cov >= minCoverage) {
                list.add(new CoverageInterval(sampleBlockPos, startIndex, endIndex, cov));
            }
        }

        if (isSortingByCov) {
            Collections.sort(list);
        }

        return list;
    }

    /* 
     a: 0-2
     b: 3-9
     c: 10-19
     d: 20-200
     e: 201
     */
    private static int getCoverageByBin(String bin) {
        if (bin.equals("b")) {
            return 3;
        } else if (bin.equals("c")) {
            return 10;
        } else if (bin.equals("d")) {
            return 20;
        } else if (bin.equals("e")) {
            return 201;
        } else {
            return Data.NA;
        }
    }
}
