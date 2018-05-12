package shi.quan.sshtest;

import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.shell.ProcessShell;
import org.apache.sshd.server.shell.TtyFilterInputStream;
import org.apache.sshd.server.shell.TtyFilterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WorkFolderSpecificProcessShell extends ProcessShell {
    private static Logger logger = LoggerFactory.getLogger(WorkFolderSpecificProcessShell.class);


    private File workFolder;

    public WorkFolderSpecificProcessShell(File workFolder, String... command) {
        super(command);
        logger.info("[WorkFolderSpecificProcessShell.<init>] {}", Arrays.asList(command.clone()));
        this.workFolder = workFolder;
    }

    public WorkFolderSpecificProcessShell(File workFolder, Collection<String> command) {
        super(command);
        logger.info("[WorkFolderSpecificProcessShell.<init>] {}", command);
        this.workFolder = workFolder;
    }

    @Override
    public void start(Environment env) throws IOException {
        logger.info("[WorkFolderSpecificProcessShell.start]");

        List<String> command = null;
        String cmdValue;
        Process process;
        TtyFilterInputStream out, err;
        TtyFilterOutputStream in;

        try {
            Field commandField = ProcessShell.class.getDeclaredField("command");
            commandField.setAccessible(true);
            command = (List<String>) commandField.get(this);

            Field cmdValueField = ProcessShell.class.getDeclaredField("cmdValue");
            cmdValueField.setAccessible(true);
            cmdValue = (String) cmdValueField.get(this);

            Field processField = ProcessShell.class.getDeclaredField("process");
            processField.setAccessible(true);
            process = (Process) processField.get(this);
        } catch (NoSuchFieldException e) {
            throw new IOException(e);
        } catch (IllegalAccessException e) {
            throw new IOException(e);
        }


        Map<String, String> varsMap = resolveShellEnvironment(env.getEnv());
        for (int i = 0; i < command.size(); i++) {
            String cmd = command.get(i);
            if ("USER".equals(cmd)) {
                cmd = varsMap.get("USER");
                command.set(i, cmd);
                cmdValue = GenericUtils.join(command, ' ');
            }
        }

        ProcessBuilder builder = new ProcessBuilder(command);

        if (GenericUtils.size(varsMap) > 0) {
            try {
                Map<String, String> procEnv = builder.environment();
                procEnv.putAll(varsMap);
            } catch (Exception e) {
                log.warn("start() - Failed ({}) to set environment for command={}: {}",
                        e.getClass().getSimpleName(), cmdValue, e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("start(" + cmdValue + ") failure details", e);
                }
            }
        }

        if(this.workFolder != null && this.workFolder.exists()) {
            builder.directory(this.workFolder);
        }

        if (log.isDebugEnabled()) {
            log.debug("Starting shell with command: '{}', env: {} and directory: {}", builder.command(), builder.environment(), builder.directory());
        }

        process = builder.start();

        Map<PtyMode, ?> modes = resolveShellTtyOptions(env.getPtyModes());
        out = new TtyFilterInputStream(process.getInputStream(), modes);
        err = new TtyFilterInputStream(process.getErrorStream(), modes);
        in = new TtyFilterOutputStream(process.getOutputStream(), err, modes);

        try {
            Field commandField = ProcessShell.class.getDeclaredField("command");
            commandField.setAccessible(true);
            commandField.set(this, command);

            Field cmdValueField = ProcessShell.class.getDeclaredField("cmdValue");
            cmdValueField.setAccessible(true);
            cmdValueField.set(this, cmdValue);

            Field processField = ProcessShell.class.getDeclaredField("process");
            processField.setAccessible(true);
            processField.set(this, process);

            Field inField = ProcessShell.class.getDeclaredField("in");
            inField.setAccessible(true);
            inField.set(this, in);

            Field outField = ProcessShell.class.getDeclaredField("out");
            outField.setAccessible(true);
            outField.set(this, out);

            Field errField = ProcessShell.class.getDeclaredField("err");
            errField.setAccessible(true);
            errField.set(this, err);
        } catch (NoSuchFieldException e) {
            throw new IOException(e);
        } catch (IllegalAccessException e) {
            throw new IOException(e);
        }
    }
}
