package shi.quan.sshtest;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.forward.DefaultForwarderFactory;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
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

                workFolder = workFolder.getAbsoluteFile().getCanonicalFile();

                if(!workFolder.exists()) {
                    workFolder.mkdirs();
                }

                logger.info("workfolder : {}", workFolder);

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

        DefaultForwarderFactory forwarderFactory = new DefaultForwarderFactory();

        forwarderFactory.addPortForwardingEventListener(new PortForwardingEventListener() {
            @Override
            public void establishingExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding) throws IOException {
                logger.info("[establishingExplicitTunnel] user : {}, local : {}, remote : {}, localForwarding : {}", session.getUsername(), local.toString(), remote.toString(), localForwarding);
            }

            @Override
            public void establishedExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding, SshdSocketAddress boundAddress, Throwable reason) throws IOException {
                logger.info("[] user : {}, local : {}, remote : {}, localForwarding : {}", session.getUsername(), local.toString(), remote.toString(), localForwarding);

            }

            @Override
            public void tearingDownExplicitTunnel(Session session, SshdSocketAddress address, boolean localForwarding) throws IOException {
                logger.info("[tearingDownExplicitTunnel] user : {}, address : {}, localForwarding : {}", session.getUsername(), address.toString(), localForwarding);

            }

            @Override
            public void tornDownExplicitTunnel(Session session, SshdSocketAddress address, boolean localForwarding, Throwable reason) throws IOException {
                logger.info("[tornDownExplicitTunnel] user : {}, address : {}, localForwarding : {}", session.getUsername(), address.toString(), localForwarding);

                if(reason != null) {
                    logger.info("reason : ", reason);
                }

            }

            @Override
            public void establishingDynamicTunnel(Session session, SshdSocketAddress local) throws IOException {
                logger.info("[establishingDynamicTunnel] user : {}, local : {}", session.getUsername(), local.toString());

            }

            @Override
            public void establishedDynamicTunnel(Session session, SshdSocketAddress local, SshdSocketAddress boundAddress, Throwable reason) throws IOException {
                logger.info("[establishedDynamicTunnel] user : {}, local : {}, remote : {}", session.getUsername(), local.toString(), boundAddress.toString());
                if(reason != null) {
                    logger.info("reason : ", reason);
                }

            }

            @Override
            public void tearingDownDynamicTunnel(Session session, SshdSocketAddress address) throws IOException {
                logger.info("[tearingDownDynamicTunnel] user : {}, address : {}", session.getUsername(), address.toString());

            }

            @Override
            public void tornDownDynamicTunnel(Session session, SshdSocketAddress address, Throwable reason) throws IOException {
                logger.info("[tornDownDynamicTunnel] user : {}, address : {}", session.getUsername(), address.toString());

                if(reason != null) {
                    logger.info("reason : ", reason);
                }
            }
        });
        sshd.setForwardingFilter(new AcceptAllForwardingFilter());
        sshd.setForwarderFactory(forwarderFactory);


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
