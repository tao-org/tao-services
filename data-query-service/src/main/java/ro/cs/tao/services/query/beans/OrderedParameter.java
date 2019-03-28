package ro.cs.tao.services.query.beans;


import ro.cs.tao.services.model.datasource.ParameterDescriptor;

public class OrderedParameter extends ParameterDescriptor {
    private int order;

    public OrderedParameter(ParameterDescriptor base, int order) {
        super(base.getName(), base.getLabel(), base.getDescription(), base.getType(),
              base.getDefaultValue(), base.isRequired(), base.getValueSet());
        this.order = order;
    }

    public int getOrder() { return order; }
}
