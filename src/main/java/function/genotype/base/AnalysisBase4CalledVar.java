package function.genotype.base;

import function.variant.base.AnalysisBase4Variant;
import global.Data;
import function.variant.base.RegionManager;
import function.variant.base.VariantManager;

/**
 *
 * @author nick
 */
public abstract class AnalysisBase4CalledVar extends AnalysisBase4Variant {

    private CalledVariant calledVar;

    public abstract void processVariant(CalledVariant calledVar);

    @Override
    public void processDatabaseData() throws Exception {
        totalNumOfRegionList = RegionManager.getRegionSize();

        for (int r = 0; r < totalNumOfRegionList; r++) {

            for (String varType : Data.VARIANT_TYPE) {

                if (VariantManager.isVariantTypeValid(r, varType)) {

                    isIndel = varType.equals("indel");

                    calledVar = null;

                    analyzedRecords = 0;

                    region = RegionManager.getRegion(r, varType);

                    rset = getAnnotationList(varType, region);

                    while (rset.next()) {
                        annotation.init(rset, isIndel);

                        if (annotation.isValid()) {

                            nextVariantId = rset.getInt(varType + "_id");

                            if (calledVar == null
                                    || nextVariantId != calledVar.getVariantId()) {
                                processVariant();

                                calledVar = new CalledVariant(nextVariantId, isIndel, rset);
                            } // end of new one

                            calledVar.update(annotation);
                        }
                    }

                    processVariant(); // only for the last qualified variant

                    printTotalAnnotationCount(varType);

                    clearData();
                }
            }

            doOutput(); // only comphet function support it
        }
    }

    private void processVariant() {
        if (calledVar != null
                && calledVar.isValid()
                && !VariantManager.isVariantOutput(calledVar.variantId)) {
            calledVar.initKnownVar();
            
            processVariant(calledVar);

            countVariant();
        }
    }
}
