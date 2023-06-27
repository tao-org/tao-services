package ro.cs.tao.services.jupyter.model;

import java.util.List;
import java.util.Map;

public class Kernel {
    private String display_name;
    private String language;
    private List<String> agrv;
    private Map<String, String> env;

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getAgrv() {
        return agrv;
    }

    public void setAgrv(List<String> agrv) {
        this.agrv = agrv;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }
}
