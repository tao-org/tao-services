package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.ogc.model.processes.core.ProcessSummary;

public class StaticIndicator extends ProcessSummary {
    private boolean mutable;

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }
}
