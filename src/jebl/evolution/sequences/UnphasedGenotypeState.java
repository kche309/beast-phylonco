package jebl.evolution.sequences;

import phylonco.evolution.datatype.UnphasedGenotype;

public class UnphasedGenotypeState extends State {

    public UnphasedGenotypeState(String name, String stateCode, int index) {
        super(name, stateCode, index);
    }

    public UnphasedGenotypeState(String name, String stateCode, int index, State[] ambiguities) {
        super(name, stateCode, index, ambiguities);
    }

    @Override
    public boolean isGap() {
        return this == UnphasedGenotype.GAP_STATE;
    }

    @Override
    public SequenceType getType() {
        return UnphasedGenotype.INSTANCE;
    }
}
