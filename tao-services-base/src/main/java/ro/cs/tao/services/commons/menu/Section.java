package ro.cs.tao.services.commons.menu;

import ro.cs.tao.TaoEnum;

public enum Section implements TaoEnum<Integer> {
    DATASETS("My Datasets", 1),
    WORKFLOWS("My Workflows", 2),
    COMPONENTS("My Components", 3),
    ADMINISTRATION("Admin area", 4);


    private final String title;
    private final int position;

    Section(String value, int position) {
        this.title = value;
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String friendlyName() {
        return title;
    }

    @Override
    public Integer value() {
        return position;
    }
}
