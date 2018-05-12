package shi.quan.sshtest;

import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.server.shell.InvertedShell;

import java.io.File;

public class WorkFolderSpecificInteractiveProcessShellFactory extends InteractiveProcessShellFactory {
    private File workFolder;

    public WorkFolderSpecificInteractiveProcessShellFactory(File workFolder) {
        super();
        this.workFolder = workFolder;
    }

    @Override
    protected InvertedShell createInvertedShell() {
        //return new WorkFolderSpecificProcessShell(this.workFolder, resolveEffectiveCommand(getCommand()));

        return new WorkFolderSpecificProcessShell(this.workFolder, "/bin/sh", "-i");
    }
}
