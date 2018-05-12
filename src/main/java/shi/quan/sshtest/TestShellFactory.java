package shi.quan.sshtest;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TestShellFactory implements Factory<Command> {
    private static Logger logger = LoggerFactory.getLogger(TestShellFactory.class);

    private Command _command;

    private File workfolder;

    public TestShellFactory(File workfolder) {
        this.workfolder = workfolder;
    }

    @Override
    public Command get() {
        if (_command == null) {
            _command = create();
        }

        return _command;
    }

    @Override
    public Command create() {
        return new TestShell(this.workfolder);
    }
}
