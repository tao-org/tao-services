package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.ogc.model.common.Link;

import java.util.List;

public class Input {
    private List<InputValueNoObject> inputValueNoObjects;
    private List<QualifiedInputValue> qualifiedInputValues;
    private List<Link> links;

    public List<InputValueNoObject> getInputValueNoObjects() {
        return inputValueNoObjects;
    }

    public void setInputValueNoObjects(List<InputValueNoObject> inputValueNoObjects) {
        this.inputValueNoObjects = inputValueNoObjects;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<QualifiedInputValue> getQualifiedInputValues() {
        return qualifiedInputValues;
    }

    public void setQualifiedInputValues(List<QualifiedInputValue> qualifiedInputValues) {
        this.qualifiedInputValues = qualifiedInputValues;
    }
}
