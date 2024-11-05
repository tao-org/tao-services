package ro.cs.tao.ogc.model.common;

public class Link {
    private String href;
    private String rel;
    private String type;
    private String hrefLang;
    private String title;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getHrefLang() {
        return hrefLang;
    }

    public void setHrefLang(String hrefLang) {
        this.hrefLang = hrefLang;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
