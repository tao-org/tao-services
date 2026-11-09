package ro.cs.tao.services.zeppelin.model.notebook;

import com.fasterxml.jackson.annotation.JsonFormat;
import ro.cs.tao.services.zeppelin.model.paragraph.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotebookParagraph {
    private String text;
    private String user = "user";
    private String id;
    private String jobname;
    private String progress = "0";
    private int progressUpdateIntervalMs = 500;
    private ParagraphConfig config = new ParagraphConfig();
    private ParagraphSettings settings = new ParagraphSettings();
    private ParagraphResults results = new ParagraphResults();
    private ParagraphRuntimeInfo runtimeInfos = new ParagraphRuntimeInfo();
    private List<ParagraphApplications> apps = new ArrayList<>();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime dateUpdated = LocalDateTime.now();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime dateCreated = LocalDateTime.now();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime dateStarted = LocalDateTime.now();
    private ParagraphStatus status = ParagraphStatus.READY;


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobname() {
        return jobname;
    }

    public void setJobname(String jobname) {
        this.jobname = jobname;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public int getProgressUpdateIntervalMs() {
        return progressUpdateIntervalMs;
    }

    public void setProgressUpdateIntervalMs(int progressUpdateIntervalMs) {
        this.progressUpdateIntervalMs = progressUpdateIntervalMs;
    }

    public ParagraphConfig getConfig() {
        return config;
    }

    public void setConfig(ParagraphConfig config) {
        this.config = config;
    }

    public ParagraphSettings getSettings() {
        return settings;
    }

    public void setSettings(ParagraphSettings settings) {
        this.settings = settings;
    }

    public ParagraphResults getResults() {
        return results;
    }

    public void setResults(ParagraphResults results) {
        this.results = results;
    }

    public ParagraphRuntimeInfo getRuntimeInfos() {
        return runtimeInfos;
    }

    public void setRuntimeInfos(ParagraphRuntimeInfo runtimeInfos) {
        this.runtimeInfos = runtimeInfos;
    }

    public List<ParagraphApplications> getApps() {
        return apps;
    }

    public void setApps(List<ParagraphApplications> apps) {
        this.apps = apps;
    }

    public void addApp(ParagraphApplications app){
        if(apps == null)
            apps = new ArrayList<>();
        apps.add(app);
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(LocalDateTime dateStarted) {
        this.dateStarted = dateStarted;
    }

    public ParagraphStatus getStatus() {
        return status;
    }

    public void setStatus(ParagraphStatus status) {
        this.status = status;
    }
}
