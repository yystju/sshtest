package shi.quan.sshtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:/application.properties")
public class MAIN {
    private static Logger logger = LoggerFactory.getLogger(MAIN.class);

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(MAIN.class, args);
    }

    @Autowired
    SSHService sshService;

    @Bean
    CommandLineRunner runner() {
        return (args) -> {
            sshService.start();

            System.out.println("Press any key...");
            System.in.read();

            sshService.shutdown();
        };
    }

//    public static void main_client(String[] args) {
//        try {
//            try(SshClient client = SshClient.setUpDefaultClient()) {
//                client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
//                client.start();
//
//                client.setForwardingFilter(new AcceptAllForwardingFilter());
//                client.setForwarderFactory(new DefaultForwarderFactory());
//
//                client.setFilePasswordProvider(new FilePasswordProvider() {
//                    @Override
//                    public String getPassword(String s) throws IOException {
//                        logger.info("s : {}", s);
//                        return "passw0rd";
//                    }
//                });
//
//                ConnectFuture connectFuture = client.connect("pi", "192.168.31.234", 22);
//
//                connectFuture.await();
//
//                try(ClientSession session = connectFuture.getSession()) {
//                    session.addPasswordIdentity("passw0rd");
//
//                    session.auth().verify(10000);
//
//                    try(ChannelShell channel = session.createShellChannel()) {
//                        logger.info("channel.getPtyType() : {}", channel.getPtyType());
//                        logger.info("channel.getPtyModes() : {}", channel.getPtyModes());
//
//                        channel.setPtyType("xterm-256color");
//
//                        Map<PtyMode, Integer> modes = channel.getPtyModes();
//
//                        for(PtyMode key : modes.keySet()) {
//                            if(key == PtyMode.ECHO) {
//                                modes.put(key, 0);
//                            } else if(key == PtyMode.ECHOCTL) {
//                                modes.put(key, 0);
//                            } else if(key == PtyMode.ECHOE) {
//                                modes.put(key, 0);
//                            } else if(key == PtyMode.ECHOK) {
//                                modes.put(key, 0);
//                            } else if(key == PtyMode.ECHOKE) {
//                                modes.put(key, 0);
//                            } else if(key == PtyMode.ECHONL) {
//                                modes.put(key, 0);
//                            }
//                        }
//
//                        channel.setPtyModes(modes);
//
//                        channel.setIn(new NoCloseInputStream(System.in));
//                        channel.setOut(new NoCloseOutputStream(System.out));
//                        channel.setErr(new NoCloseOutputStream(System.err));
//
//                        channel.open();
//
//                        channel.waitFor(Arrays.asList(new ClientChannelEvent[]{ClientChannelEvent.CLOSED}), 0);
//                    } finally {
//                        session.close(false);
//                    }
//                } finally {
//                    client.stop();
//                }
//            }
//        } catch(Exception ex) {
//            logger.error(ex.getMessage(), ex);
//        }
//    }
//
//    public static void main_server(String[] args) {
//        try {
//            SshServer sshd = SshServer.setUpDefaultServer();
//
//            sshd.setPort(2222);
//
//            File keyPairFile = new File("./keypair.ser");
//
//            if(!keyPairFile.exists()) {
//                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
//
//                KeyPair keyPair = generator.generateKeyPair();
//
//                ObjectOutputStream outs = new ObjectOutputStream(new FileOutputStream(keyPairFile));
//
//                outs.writeObject(keyPair);
//
//                outs.flush();
//
//                outs.close();
//            }
//
//            File workfolder = new File("/home/quan/root");
//
//            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyPairFile));
//
////            sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));
//
//            InteractiveProcessShellFactory shellFactory = new InteractiveProcessShellFactory();
//
//            sshd.setShellFactory(shellFactory);
//
//            VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory();
//
//            fileSystemFactory.setDefaultHomeDir(workfolder.toPath());
//
//            fileSystemFactory.setUserHomeDir("root", workfolder.toPath());
//
//            sshd.setFileSystemFactory(fileSystemFactory);
//
//            ProcessCommandFactory processCommandFactory = new ProcessCommandFactory(workfolder);
//
//            ScpCommandFactory scpCommandFactory = new ScpCommandFactory();
//
//            scpCommandFactory.setDelegateCommandFactory(processCommandFactory);
//
//            sshd.setCommandFactory(scpCommandFactory);
//
//            sshd.setForwarderFactory(DefaultForwarderFactory.INSTANCE);
//
//            sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
//                public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) {
//                    //logger.info("[PublickeyAuthenticator.authenticate] s : {}, publicKey: {}, serverSession : {}", s, publicKey, serverSession);
//                    return false;
//                }
//            });
//
//            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
//                public boolean authenticate(String name, String passwd, ServerSession serverSession) throws PasswordChangeRequiredException {
//                    logger.info("[PasswordAuthenticator.authenticate] name : {}, passwd: {}, client : {}", name, passwd, serverSession.getClientAddress());
//                    return "root".equals(name) && "passw0rd".equals(passwd);
//                }
//            });
//
//            sshd.start();
//
//            System.out.println("Press any key to stop...");
//
//            System.in.read();
//
//            sshd.close();
//
//            processCommandFactory.shutdown();
//        } catch(Exception ex) {
//            logger.error(ex.getMessage(), ex);
//        }
//    }
}
