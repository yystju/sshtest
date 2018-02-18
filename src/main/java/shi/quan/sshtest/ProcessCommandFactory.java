package shi.quan.sshtest;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class ProcessCommandFactory implements CommandFactory {
    private ExecutorService executorService;
    private File workfolder;

    public ProcessCommandFactory(ExecutorService executorService, File workfolder) {
        this.executorService = executorService;
        this.workfolder = workfolder;
    }

    @Override
    public Command createCommand(String command) {
        Command ret = new ProcessCommand(executorService, command, workfolder);
        return ret;
    }

    public void shutdown() {
        this.executorService.shutdown();
    }
}
