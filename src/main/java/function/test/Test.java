package function.test;

import function.AnalysisBase;
import utils.ErrorManager;

/**
 * code here for any testing or small ETL task purpose
 *
 * @author nick
 */
public class Test extends AnalysisBase {

    @Override
    public void initOutput() {
    }

    @Override
    public void closeOutput() {
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
    public void processDatabaseData() {
        try {
//            CreateVDSDB.run();

//            OutputSubsetSample.run();
//            
//            LoadSubsetSample.run();
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    @Override
    public String toString() {
        return "Start running test function";
    }
}
