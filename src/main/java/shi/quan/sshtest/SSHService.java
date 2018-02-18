package shi.quan.sshtest;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.forward.DefaultForwarderFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SSHService {
    private static Logger logger = LoggerFactory.getLogger(SSHService.class);

    @Value("${ssh.port}")
    private int port;

    @Value("${ssh.name}")
    private String userName;

    @Value("${ssh.password}")
    private String password;

    @Value("${ssh.workfolder}")
    private File workFolder;



    private SshServer sshd;
    private ExecutorService executorService;
    private boolean hasBeenStarted = false;


    public void start() throws IOException, NoSuchAlgorithmException {
        synchronized (SSHService.class) {
            if(!hasBeenStarted) {
                hasBeenStarted = true;

                workFolder = workFolder.getAbsoluteFile();

                if(!workFolder.exists()) {
                    workFolder.mkdirs();
                }

                executorService = Executors.newCachedThreadPool();

                internalStart();
            }
        }
    }

    public void shutdown() throws IOException {
        synchronized (SSHService.class) {
            if(hasBeenStarted) {
                sshd.close();

                executorService.shutdown();

                hasBeenStarted = false;
            }
        }
    }

    private void internalStart() throws IOException, NoSuchAlgorithmException {
        this.sshd = SshServer.setUpDefaultServer();

        sshd.setPort(port);

        File keyPairFile = new File(workFolder,"keypair.ser");

        if(!keyPairFile.exists()) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

            KeyPair keyPair = generator.generateKeyPair();

            ObjectOutputStream outs = new ObjectOutputStream(new FileOutputStream(keyPairFile));

            outs.writeObject(keyPair);

            outs.flush();

            outs.close();
        }

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyPairFile));


        WorkFolderSpecificInteractiveProcessShellFactory shellFactory = new WorkFolderSpecificInteractiveProcessShellFactory(this.workFolder);
        sshd.setShellFactory(shellFactory);


        VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory();
        fileSystemFactory.setDefaultHomeDir(workFolder.toPath());
        fileSystemFactory.setUserHomeDir(userName, workFolder.toPath());
        sshd.setFileSystemFactory(fileSystemFactory);

        ProcessCommandFactory processCommandFactory = new ProcessCommandFactory(executorService, workFolder);
        ScpCommandFactory scpCommandFactory = new ScpCommandFactory();
        scpCommandFactory.setExecutorService(executorService);
        scpCommandFactory.setDelegateCommandFactory(processCommandFactory);
        sshd.setCommandFactory(scpCommandFactory);
        sshd.setForwarderFactory(DefaultForwarderFactory.INSTANCE);

        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) {
                logger.debug("[PublickeyAuthenticator.authenticate] s : {}, publicKey: {}, serverSession : {}", s, publicKey, serverSession);
                return false;
            }
        });

        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String name, String passwd, ServerSession serverSession) throws PasswordChangeRequiredException {
                logger.debug("[PasswordAuthenticator.authenticate] name : {}, passwd: {}, client : {}", name, passwd, serverSession.getClientAddress());
                return userName.equals(name) && password.equals(passwd);
            }
        });

        sshd.start();
    }
}
