package ro.cs.tao.services.commons.menu;

import ro.cs.tao.TaoEnum;

public enum Section implements TaoEnum<Integer> {
    DASHBOARD("Dashboard", 1),
    DATASETS("My Datasets", 2),
    WORKFLOWS("My Workflows", 3),
    COMPONENTS("My Components", 4),
    ADMINISTRATION("Admin area", 5);


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
