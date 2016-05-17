package function.genotype.statistics;

import function.genotype.base.AnalysisBase4CalledVar;
import function.genotype.base.CalledVariant;
import java.io.BufferedWriter;
import java.io.FileWriter;
import utils.CommonCommand;
import utils.ErrorManager;
import utils.ThirdPartyToolManager;

/**
 *
 * @author nick
 */
public class LogisticRegression extends AnalysisBase4CalledVar {

    String[] originalPOutputPath = new String[StatisticsCommand.models.length];
    BufferedWriter[] logisticBw = new BufferedWriter[StatisticsCommand.models.length];

    @Override
    public void initOutput() {
        for (int m = 0; m < StatisticsCommand.models.length; m++) {
            try {
                String testModel = StatisticsCommand.models[m];
                originalPOutputPath[m] = CommonCommand.outputPath + testModel + ".csv";
                logisticBw[m] = new BufferedWriter(new FileWriter(originalPOutputPath[m]));
                logisticBw[m].write(LogisticOutput.title);
                logisticBw[m].newLine();
            } catch (Exception ex) {
                ErrorManager.send(ex);
            }
        }
    }

    @Override
    public void doOutput() {
    }

    @Override
    public void closeOutput() {
        for (int m = 0; m < StatisticsCommand.models.length; m++) {
            try {
                logisticBw[m].flush();
                logisticBw[m].close();
            } catch (Exception ex) {
                ErrorManager.send(ex);
            }
        }
    }

    @Override
    public void doAfterCloseOutput() {
        generatePvaluesQQPlot();
    }

    @Override
    public void beforeProcessDatabaseData() {
    }

    @Override
    public void afterProcessDatabaseData() {
    }

    @Override
    public void processVariant(CalledVariant calledVar) {
        try {
            LogisticOutput output = new LogisticOutput(calledVar);
            output.countSampleGenoCov();
            output.calculate();

            for (int m = 0; m < StatisticsCommand.models.length; m++) {
                if (output.isValid(StatisticsCommand.models[m])) {
                    // needs to calculate logistic p below
                    output.doRegression(StatisticsCommand.models[m]);

                    logisticBw[m].write(output.toString());
                    logisticBw[m].newLine();
                }
            }
        } catch (Exception e) {
            ErrorManager.send(e);
        }
    }

    private void generatePvaluesQQPlot() {
        for (int m = 0; m < StatisticsCommand.models.length; m++) {
            ThirdPartyToolManager.generatePvaluesQQPlot(LogisticOutput.title,
                    "P value",
                    originalPOutputPath[m],
                    originalPOutputPath[m].replace(".csv", ".p.qq.plot.pdf"));
        }
    }
}
