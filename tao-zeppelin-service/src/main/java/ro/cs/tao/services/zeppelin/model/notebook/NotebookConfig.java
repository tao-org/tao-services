package ro.cs.tao.services.zeppelin.model.notebook;

public class NotebookConfig {

    private boolean  isZeppelinNotebookCronEnable = false;

    public boolean isZeppelinNotebookCronEnable() {
        return isZeppelinNotebookCronEnable;
    }

    public void setZeppelinNotebookCronEnable(boolean zeppelinNotebookCronEnable) {
        isZeppelinNotebookCronEnable = zeppelinNotebookCronEnable;
    }
}
