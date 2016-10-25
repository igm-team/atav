package function.genotype.base;

import function.variant.base.AnalysisBase4Variant;
import function.variant.base.RegionManager;
import function.variant.base.VariantManager;

/**
 *
 * @author nick
 */
public abstract class AnalysisBase4CalledVar extends AnalysisBase4Variant {

    private CalledVariant calledVar;

    public abstract void processVariant(CalledVariant calledVar);

    // only comphet function support it
    public abstract void doOutput();

    @Override
    public void processDatabaseData() throws Exception {
//        for (int r = 0; r < RegionManager.getRegionSize(); r++) {
//
//            for (String varType : VariantManager.VARIANT_TYPE) {
//
//                if (VariantManager.isVariantTypeValid(r, varType)) {
//
//                    isIndel = varType.equals("indel");
//
//                    calledVar = null;
//
//                    analyzedRecords = 0;
//
//                    region = RegionManager.getRegion(r, varType);
//
//                    rset = getAnnotationList(varType, region);
//
//                    while (rset.next()) {
//                        annotation.init(rset, isIndel);
//
//                        if (annotation.isValid()) {
//
//                            nextVariantId = rset.getInt(varType + "_id");
//
//                            if (calledVar == null
//                                    || nextVariantId != calledVar.getVariantId()) {
//                                processVariant();
//
//                                calledVar = new CalledVariant(nextVariantId, isIndel, rset);
//                            } // end of new one
//
//                            calledVar.update(annotation);
//                        }
//                    }
//
//                    processVariant(); // only for the last qualified variant
//
//                    printTotalAnnotationCount(varType);
//
//                    rset.close();
//                }
//            }
//
//            doOutput(); // only comphet function support it
//        }
    }

    private void processVariant() {
        if (calledVar != null
                && calledVar.isValid()) {
            calledVar.initExternalData();

            processVariant(calledVar);

            countVariant();
        }
    }
}
