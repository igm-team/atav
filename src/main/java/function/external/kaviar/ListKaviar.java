package function.external.kaviar;

import function.AnalysisBase;
import function.variant.base.Region;
import function.variant.base.RegionManager;
import function.variant.base.VariantManager;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.CommonCommand;
import utils.DBManager;
import utils.ErrorManager;

/**
 *
 * @author nick
 */
public class ListKaviar extends AnalysisBase {

    BufferedWriter bwKaviar = null;
    final String kaviarFilePath = CommonCommand.outputPath + "kaviar.csv";

    int analyzedRecords = 0;

    @Override
    public void initOutput() {
        try {
            bwKaviar = new BufferedWriter(new FileWriter(kaviarFilePath));
            bwKaviar.write(KaviarOutput.getTitle());
            bwKaviar.newLine();
        } catch (Exception ex) {
            ErrorManager.send(ex);
        }
    }

    @Override
    public void closeOutput() {
        try {
            bwKaviar.flush();
            bwKaviar.close();
        } catch (Exception ex) {
            ErrorManager.send(ex);
        }
    }

    @Override
    public void doAfterCloseOutput() {
    }

    @Override
    public void beforeProcessDatabaseData() {
    }

    @Override
    public void afterProcessDatabaseData() {
    }

    @Override
    public void processDatabaseData() throws Exception {
        int totalNumOfRegionList = RegionManager.getRegionSize();

        for (int r = 0; r < totalNumOfRegionList; r++) {

            for (String varType : VariantManager.VARIANT_TYPE) {

                if (VariantManager.isVariantTypeValid(r, varType)) {

                    boolean isIndel = varType.equals("indel");

                    Region region = RegionManager.getRegion(r, varType);
             
                    String sqlCode = KaviarManager.getSql(isIndel, region);
                    
                    ResultSet rset = DBManager.executeReadOnlyQuery(sqlCode);

                    while (rset.next()) {
                        KaviarOutput output = new KaviarOutput(isIndel, rset);

                        if (VariantManager.isVariantIdIncluded(output.kaviar.getVariantId())
                                && output.isValid()) {
                            bwKaviar.write(output.kaviar.getVariantId() + ",");
                            bwKaviar.write(output.toString());
                            bwKaviar.newLine();
                        }

                        countVariant();
                    }

                    rset.close();
                }
            }
        }
    }

    protected void countVariant() {
        analyzedRecords++;
        System.out.print("Processing variant "
                + analyzedRecords + "                     \r");
    }

    @Override
    public String toString() {
        return "Start running list kaviar function";
    }
}
