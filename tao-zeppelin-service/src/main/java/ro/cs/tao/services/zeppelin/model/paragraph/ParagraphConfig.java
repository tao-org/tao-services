package ro.cs.tao.services.zeppelin.model.paragraph;

public class ParagraphConfig {
    private double colWidth = 12.0;
    private double fontSize = 9.0;
    private boolean enabled = true;
    private ParagraphConfigEditorSetting editorSetting = new ParagraphConfigEditorSetting();

    public double getColWidth() {
        return colWidth;
    }

    public void setColWidth(double colWidth) {
        this.colWidth = colWidth;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ParagraphConfigEditorSetting getEditorSetting() {
        return editorSetting;
    }

    public void setEditorSetting(ParagraphConfigEditorSetting editorSetting) {
        this.editorSetting = editorSetting;
    }
}
