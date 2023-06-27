package ro.cs.tao.services.jupyter.model;

public class NotebookMetadata {
    private String signature;
    private Kernel kernelspec;

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
}
