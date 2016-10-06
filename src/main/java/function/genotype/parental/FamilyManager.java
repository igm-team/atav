package function.genotype.parental;

import function.genotype.base.Sample;
import function.genotype.base.SampleManager;
import utils.ErrorManager;
import utils.LogManager;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author nick
 */
public class FamilyManager {

    private static ArrayList<Family> familyList = new ArrayList<Family>();

    public static void init() {
        initList();
    }

    private static void initList() {
        HashMap<String, Family> familyMap = new HashMap<String, Family>();

        for (Sample sample : SampleManager.getList()) {
            if (!sample.getPaternalId().equals("0")
                    || !sample.getMaternalId().equals("0")) {
                if (familyMap.containsKey(sample.getFamilyId())) {
                    Family family = familyMap.get(sample.getFamilyId());
                    family.addChild(sample);
                } else {
                    Family family = new Family(sample);
                    familyMap.put(sample.getFamilyId(), family);
                }
            }
        }

        for (Family family : familyMap.values()) {
            familyList.add(family);
        }

        if (familyList.isEmpty()) {
            ErrorManager.print("Missing family in sample file");
        } else {
            LogManager.writeAndPrint("Total families: " + familyList.size());
        }
    }

    public static ArrayList<Family> getList() {
        return familyList;
    }
}
