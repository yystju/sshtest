package shi.quan.sshtest;

import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ProcessCommand implements Command {
    private ExecutorService executorService;

    private InputStream ins;
    private OutputStream outs;
    private OutputStream errs;
    private ExitCallback exitCallback;

    private String command;
    private File workfolder;

    public ProcessCommand(ExecutorService executorService, String command, File workfolder) {
        this.executorService = executorService;
        this.command = command;
        this.workfolder = workfolder;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.ins = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.outs = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.errs = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.exitCallback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        try {
            System.out.println("START PROCESS : " + command);

            Map<String, String> envMap = new HashMap<>();

            envMap.putAll(System.getenv());
            envMap.putAll(env.getEnv());

            final Process p = Runtime.getRuntime().exec(command);

//            ProcessBuilder pb = new ProcessBuilder(command);
//
//            pb.environment().putAll(envMap);
//
//            System.out.println(pb.environment());
//
//            pb.directory(workfolder);
//
//            final Process p = pb.start();

            InputStream i = p.getInputStream();
            OutputStream o = p.getOutputStream();
            InputStream e = p.getErrorStream();

            this.executorService.execute(new PipeRunnable(i, new NoCloseOutputStream(this.outs)));
            this.executorService.execute(new PipeRunnable(new NoCloseInputStream(this.ins), o));
            this.executorService.execute(new PipeRunnable(e, new NoCloseOutputStream(this.errs)));

            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        int rc = p.waitFor();

                        System.out.println("command : \"" + command + "\" end with rc : " + rc);

                        Thread.sleep(100);

                        exitCallback.onExit(rc);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("[destroy]");
    }

    private static class PipeRunnable implements Runnable {
        private InputStream ins;
        private OutputStream outs;
        private int bufferSize;

        public PipeRunnable(InputStream ins, OutputStream outs) {
            this(ins, outs, 8192);
        }

        public PipeRunnable(InputStream ins, OutputStream outs, int bufferSize) {
            this.ins = ins;
            this.outs = outs;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[this.bufferSize];

                int len = -1;

                while(-1 != (len = ins.read(buffer))) {
                    outs.write(buffer, 0, len);
                    outs.flush();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if(ins != null) {
                    try {
                        this.ins.close();
                    } catch (IOException e) {
                    }
                }

                if(outs != null) {
                    try {
                        this.outs.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
