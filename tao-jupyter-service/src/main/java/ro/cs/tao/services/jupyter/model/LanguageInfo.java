package ro.cs.tao.services.jupyter.model;

import java.util.Map;

public class LanguageInfo {
    private Map<String, Object> codemirror_mode;
    private String file_extension = ".py";
    private String mimetype = "text/x-python";
    private String name = "python";
    private String nbconvert_exporter = "python";
    private String pygments_lexer = "ipython3";
    private String version = "3.11.4";

    public void setCodemirror_mode(Map<String, Object> codemirror_mode) {
        this.codemirror_mode = codemirror_mode;
    }

    public Map<String, Object> getCodemirror_mode() {
        return this.codemirror_mode;
    }

    public void setFile_extension(String file_extension) {
        this.file_extension = file_extension;
    }

    public String getFile_extension() { return  this.file_extension; }

    public String getMimetype() {return this.mimetype; }
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }

    public String getNbconvert_exporter() { return this.nbconvert_exporter; }

    public void setNbconvert_exporter(String nbconvert_exporter) {
        this.nbconvert_exporter = nbconvert_exporter;
    }

    public String getPygments_lexer() { return this.pygments_lexer; }

    public void setPygments_lexer(String pygments_lexer) {
        this.pygments_lexer = pygments_lexer;
    }

    public String getVersion() { return this.version; }
    public void setVersion(String version) { this.version = version; }

}