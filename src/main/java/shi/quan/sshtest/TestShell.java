package shi.quan.sshtest;

import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestShell implements Command {
    private static Logger logger = LoggerFactory.getLogger(TestShell.class);

    private static ExecutorService executor = Executors.newCachedThreadPool();

    private File workdir;

    private InputStream ins;
    private OutputStream outs;
    private OutputStream errs;

    private InputStream p_ins;
    private OutputStream p_outs;
    private InputStream p_errs;

    private ExitCallback exitCallback;

    public TestShell(File workdir) {
        logger.info("[TestShell.init]");
        this.workdir = workdir;
    }

    @Override
    public void start(Environment environment) throws IOException {
        logger.info("[start] environment : {}", environment.getEnv());

        ArrayList<PtyMode> modes = new ArrayList<>();

        for(PtyMode mode : environment.getPtyModes().keySet()) {
            if (environment.getPtyModes().get(mode) == 1) {
                logger.info("mode : {} - {}", mode.toString(), environment.getPtyModes().get(mode));
                modes.add(mode);
            }
        }

        logger.info("modes : {}", modes);

        executor.submit(() -> {
            Process process = null;

            int r = 0;

            try {
                ProcessBuilder builder = new ProcessBuilder();

                builder.environment().putAll(environment.getEnv());

                process = builder.command("/bin/bash", "-i", "-l").directory(workdir).start();

                p_ins = process.getInputStream();
                p_outs = process.getOutputStream();
                p_errs = process.getErrorStream();

                executor.submit(() -> {
                    logger.info("[run1] ins : {}, p_outs : {}", ins.getClass().getSimpleName(), p_outs.getClass().getSimpleName());

                    try {
                        processDownStream(modes, ins, p_outs);
                    } catch (Exception e) {
                        logger.info("ERROR : {}", e.getMessage());
                    }
                });
                executor.submit(() -> {
                    logger.info("[run2] outs : {}, p_ins : {}", outs.getClass().getSimpleName(), p_ins.getClass().getSimpleName());

                    try {
                        processUpStream(modes, p_ins, outs);
                    } catch (Exception e) {
                        logger.info("ERROR : {}", e.getMessage());
                    }
                });

                executor.submit(() -> {
                    logger.info("[run3] errs : {}, p_errs : {}", errs.getClass().getSimpleName(), p_errs.getClass().getSimpleName());

                    try {
                        processUpStream(modes, p_errs, errs);
                    } catch (Exception e) {
                        logger.info("ERROR : {}", e.getMessage());
                    }
                });

                try {
                    r = process.waitFor();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            } catch (Throwable ex) {
                logger.error("ERROR : {}", ex.getMessage());
            } finally {
                exitCallback.onExit(process != null ? process.exitValue() : r);
            }
        });
    }

    private void processUpStream(ArrayList<PtyMode> modes, InputStream s, OutputStream o) throws IOException {
        int N = 1024 * 8;

        byte[] buffer = new byte[N * 2];
        int len = -1;
        int adjustedLen = -1;

        while (-1 != (len = s.read(buffer, 0, N))) {
            adjustedLen = len;

            for (int i = 0; i < len; ++i) {
                if (buffer[i] == '\r') {
                    if (modes.contains(PtyMode.ICRNL)) {
                        buffer[i] = '\n';
                    } else if (modes.contains(PtyMode.IGNCR)) {
                        for (int j = i; j < len; ++j) {
                            buffer[j] = buffer[j + 1];
                        }

                        --adjustedLen;
                    }
                }

                if (buffer[i] == '\n') {
                    if (modes.contains(PtyMode.INLCR)) {
                        buffer[i] = '\r';
                    } else {
                        buffer[i] = '\n';
                    }
                }
            }

            o.write(buffer, 0, adjustedLen);
            o.flush();
        }
    }

    private void processDownStream(ArrayList<PtyMode> modes, InputStream s, OutputStream o) throws IOException {
        int N = 1024 * 8;

        byte[] buffer = new byte[N * 2];
        int len = -1;
        int adjustedLen = -1;

        while (-1 != (len = s.read(buffer, 0, N))) {
            adjustedLen = len;

            for (int i = 0; i < len; ++i) {
                if (buffer[i] == '\r') {

                    if (modes.contains(PtyMode.OCRNL)) {
                        buffer[i] = '\n';
                    }
                }

                if (buffer[i] == '\n') {
                    if (((modes.contains(PtyMode.ONLCR) || modes.contains(PtyMode.ONOCR)) && (i == 0 || buffer[i - 1] != '\r'))) {
                        //TODO: Here's an assumption that there won't be enlarged packet that's more than 8kB...
                        for (int j = len; j >= i; --j) {
                            buffer[j] = buffer[j - 1];
                        }

                        buffer[i] = '\n';
                        ++adjustedLen;
                    } else if (modes.contains(PtyMode.ONLRET)) {
                        buffer[i] = '\r';
                    }
                }
            }

            o.write(buffer, 0, adjustedLen);
            o.flush();
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("[destroy]");
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.ins = inputStream;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outs = outputStream;
    }

    @Override
    public void setErrorStream(OutputStream outputStream) {
        this.errs = outputStream;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }
}
