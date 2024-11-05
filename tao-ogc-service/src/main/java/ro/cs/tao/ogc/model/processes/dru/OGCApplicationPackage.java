package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.ogc.model.common.Link;
import ro.cs.tao.ogc.model.processes.core.Process;
import ro.cs.tao.ogc.model.processes.core.QualifiedInputValue;

import java.util.Arrays;
import java.util.List;

public class OGCApplicationPackage<E> {
    private E unit;
    private Process processDescription;

    public static OGCApplicationPackage<ExecutionUnit> of(ExecutionUnit unit) {
        final OGCApplicationPackage<ExecutionUnit> p = new OGCApplicationPackage<>();
        p.setUnit(unit);
        return p;
    }

    public static OGCApplicationPackage<List<ExecutionUnit>> of(ExecutionUnit... units) {
        final OGCApplicationPackage<List<ExecutionUnit>> p = new OGCApplicationPackage<>();
        p.setUnit(Arrays.asList(units));
        return p;
    }

    public static OGCApplicationPackage<Link> of(Link link) {
        final OGCApplicationPackage<Link> p = new OGCApplicationPackage<>();
        p.setUnit(link);
        return p;
    }

    public static OGCApplicationPackage<List<Link>> of(Link... units) {
        final OGCApplicationPackage<List<Link>> p = new OGCApplicationPackage<>();
        p.setUnit(Arrays.asList(units));
        return p;
    }

    public static <E> OGCApplicationPackage<QualifiedInputValue<E>> of(QualifiedInputValue<E> value) {
        final OGCApplicationPackage<QualifiedInputValue<E>> p = new OGCApplicationPackage<>();
        p.setUnit(value);
        return p;
    }

    public static OGCApplicationPackage<List<QualifiedInputValue<?>>> of(QualifiedInputValue<?>... units) {
        final OGCApplicationPackage<List<QualifiedInputValue<?>>> p = new OGCApplicationPackage<>();
        p.setUnit(Arrays.asList(units));
        return p;
    }

    OGCApplicationPackage() {
    }

    public E getUnit() {
        return unit;
    }

    public void setUnit(E unit) {
        this.unit = unit;
    }

    public Process getProcessDescription() {
        return processDescription;
    }

    public void setProcessDescription(Process processDescription) {
        this.processDescription = processDescription;
    }
}
