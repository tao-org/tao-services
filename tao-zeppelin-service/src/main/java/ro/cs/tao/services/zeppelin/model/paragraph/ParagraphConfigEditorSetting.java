package ro.cs.tao.services.zeppelin.model.paragraph;

public class ParagraphConfigEditorSetting {
    private String language = "python";
    private boolean editOnDblClick = false;
    private boolean completionSupport = true;

    public String getLanguage() {
        return language;
    }

    public boolean isEditOnDblClick() {
        return editOnDblClick;
    }

    public boolean isCompletionSupport() {
        return completionSupport;
    }
}
