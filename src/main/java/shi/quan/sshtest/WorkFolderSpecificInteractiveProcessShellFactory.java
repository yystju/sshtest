package shi.quan.sshtest;

import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.server.shell.InvertedShell;

import java.io.File;

public class WorkFolderSpecificInteractiveProcessShellFactory extends InteractiveProcessShellFactory {
    private File workFolder;

    public WorkFolderSpecificInteractiveProcessShellFactory(File workFolder) {
        this.workFolder = workFolder;
    }

    @Override
    protected InvertedShell createInvertedShell() {
        return new WorkFolderSpecificProcessShell(this.workFolder, resolveEffectiveCommand(getCommand()));
    }
}
