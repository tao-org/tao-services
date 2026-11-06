package ro.cs.tao.services.workspace.impl;

public interface UserTransferListener {
    void onTransferCompleted(String user, String srcPath, String dstPath);
}
