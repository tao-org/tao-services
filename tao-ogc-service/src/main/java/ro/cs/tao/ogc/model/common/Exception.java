package ro.cs.tao.ogc.model.common;

import org.springframework.http.HttpStatus;

public class Exception {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;

    public Exception(Throwable e, int status) {
        this.title = e.getClass().getSimpleName();
        this.detail = e.getMessage();
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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
