package ro.cs.tao.services.commons.menu;

public enum Section {
    DATASETS("My Datasets"),
    WORKFLOWS("My Workflows"),
    COMPONENTS("My Components"),
    ADMINISTRATION("Admin area");


    private final String title;

    Section(String value) {
        this.title = value;
    }

    public String getTitle() {
        return title;
    }

}
