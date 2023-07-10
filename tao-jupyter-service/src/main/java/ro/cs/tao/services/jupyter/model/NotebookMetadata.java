package ro.cs.tao.services.jupyter.model;

public class NotebookMetadata {
    private String signature;
    private Kernel kernelspec;

    private LanguageInfo language_info;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Kernel getKernelspec() {
        return kernelspec;
    }

    public void setKernelspec(Kernel kernelspec) {
        this.kernelspec = kernelspec;
    }

    public void setLanguage_info(LanguageInfo language_info) { this.language_info = language_info; }

    public LanguageInfo getLanguage_info() { return this.language_info; }
}
