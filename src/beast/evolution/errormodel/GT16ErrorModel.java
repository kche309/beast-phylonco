package beast.evolution.errormodel;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.NucleotideDiploid16;

import static beast.evolution.datatype.DataType.GAP_CHAR;
import static beast.evolution.datatype.DataType.MISSING_CHAR;

/**
 * Implements the GT16 error model for diploid genotypes from Kozlov et al. (2021)
 *
 * CellPhy: accurate and fast probabilistic inference of single-cell phylogenies from scDNA-seq data
 * https://doi.org/10.1101/2020.07.31.230292
 *
 */
public class GT16ErrorModel extends ErrorModel {

    final public Input<RealParameter> deltaInput = new Input<>("delta", "the allelic dropout probability", Input.Validate.REQUIRED);
    final public Input<RealParameter> epsilonInput = new Input<>("epsilon", "the sequencing error probability",  Input.Validate.REQUIRED);

    private RealParameter delta;
    private RealParameter epsilon;

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        delta = deltaInput.get();
        epsilon = epsilonInput.get();
    }

    @Override
    public double getProbability(int observedState, int trueState) {
        double d = delta.getValue();
        double e = epsilon.getValue();
        int states = datatype.getStateCount();
        double prob = 0.0;

        if (observedState == GAP_CHAR || observedState == MISSING_CHAR) {
            // gap or missing code
            prob = 1.0 / states;
        } else if (datatype.isAmbiguousCode(observedState)) {
            // ambiguous code for more than one state (not gap or missing)
            int[] codes = datatype.getStatesForCode(observedState);
            int numCodes = codes.length;
            double weight = 1.0 / numCodes;
            prob = 0.0;
            for (int i = 0; i < numCodes; i++) {
                prob += weight * getProbabilityUnambiguous(codes[i], trueState);
            }
//            System.out.println("full code: " + observedState + ", p = " + prob);
        } else {
            // unambiguous code
            prob = getProbabilityUnambiguous(observedState, trueState);
        }

        return prob;
    }

    private double getProbabilityUnambiguous(int observedState, int trueState) {
        double d = delta.getValue();
        double e = epsilon.getValue();

        int bases = 4;

        int trueFirst = trueState / bases; // first allele in true state
        int trueSecond = trueState % bases; // second allele in true state
        int observedFirst = observedState / bases; // first allele in observed state
        int observedSecond = observedState % bases; // second allele in observed state

        double prob = 0.0;

        if (trueFirst == trueSecond) {
            // true state homozygous
            if (observedState == trueState) {
                // P(aa | aa) = (1 - epsilon) + (1/2) * delta * epsilon
                prob = 1 - e + (1.0 / 2.0) * d * e;
            } else if (observedFirst == trueFirst || observedSecond == trueSecond) {
                // P(ab | aa) =
                // P(ba | aa) = (1 - delta) * (1/6) * epsilon
                prob = (1 - d) * (1.0 / 6.0) * e;
            } else if (observedFirst == observedSecond) {
                // P(bb | aa) = (1/6) * delta * epsilon
                prob = (1.0 / 6.0) * d * e;
            } else {
                // P(bc | aa) = 0
                prob = 0.0;
            }
        } else {
            // true state heterozygous
            if (observedState == trueState) {
                // P(ab | ab) = (1 - delta) * (1 - epsilon)
                prob = (1 - d) * (1 - e);
            } else if (observedFirst == observedSecond) {
                // observed is homozygous
                if (observedFirst == trueFirst || observedSecond == trueSecond) {
                    // P(aa | ab) =
                    // P(bb | ab) = (1/2) * delta + (1/6) * epsilon - (1/3) * delta * epsilon
                    prob = (1.0 / 2.0) * d + (1.0 / 6.0) * e - (1.0 / 3.0) * d * e;
                } else {
                    // P(cc | ab) = (1/6) * delta * epsilon
                    prob = (1.0 / 6.0) * d * e;
                }
            } else {
                // observed in heterozyous
                if (observedFirst == trueFirst || observedSecond == trueSecond) {
                    // P(ac | ab) =
                    // P(cb | ab) = (1 - delta) * (1/6) * epsilon
                    prob = (1 - d) * (1.0 / 6.0) * e;
                } else {
                    // P(cd | ab) = 0
                    prob = 0.0;
                }
            }
        }

        return prob;
    }

    @Override
    public double[] getProbabilities(int observedState) {
        int states = datatype.getStateCount();
        double[] prob = new double[states];
        for (int i = 0; i < states; i++) {
            prob[i] = getProbability(observedState, i);
        }
        return prob;
    }

    @Override
    public boolean canHandleDataType(DataType datatype) {
        return datatype instanceof NucleotideDiploid16;
    }

}
