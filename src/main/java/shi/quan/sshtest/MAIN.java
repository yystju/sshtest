package shi.quan.sshtest;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.forward.DefaultForwarderFactory;
import org.apache.sshd.server.*;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Arrays;

@SpringBootApplication
@PropertySource("classpath:/application.properties")
public class MAIN {
    private static Logger logger = LoggerFactory.getLogger(MAIN.class);

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(MAIN.class, args);
    }

    @Bean
    CommandLineRunner runner() {
        return (args) -> {
            main_server(args);
        };
    }

    public static void main_client(String[] args) {
        try {
            try(SshClient client = SshClient.setUpDefaultClient()) {
                client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
                client.start();

                client.setForwardingFilter(new AcceptAllForwardingFilter());
                client.setForwarderFactory(new DefaultForwarderFactory());

                client.setFilePasswordProvider(new FilePasswordProvider() {
                    @Override
                    public String getPassword(String s) throws IOException {
                        logger.info("s : {}", s);
                        return "passw0rd";
                    }
                });

                ConnectFuture connectFuture = client.connect("pi", "192.168.31.234", 22);

                connectFuture.await();

                try(ClientSession session = connectFuture.getSession()) {
                    session.addPasswordIdentity("passw0rd");

                    session.auth().verify(10000);

                    try(ChannelShell channel = session.createShellChannel()) {
                        logger.info("channel.getPtyType() : {}", channel.getPtyType());
                        logger.info("channel.getPtyModes() : {}", channel.getPtyModes());
//                        channel.setIn((System.in));
//                        channel.setOut((System.out));

//                        channel.setOut(new OutputStream() {
//                            ByteArrayOutputStream outs = new ByteArrayOutputStream();
//
//                            @Override
//                            public void write(int b) throws IOException {
//                                outs.write(b);
//
//                                if(outs.toString().endsWith("\n")) {
//                                    System.out.print(new String(outs.toByteArray()));
//                                    outs.reset();
//                                }
//                            }
//                        });

                        channel.setOut((System.out));
                        channel.setIn(System.in);
//                        channel.setOut(new OutputStream() {
//                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                            @Override
//                            public void write(int b) throws IOException {
//                                outputStream.write(b);
//
//                                String str = new String(outputStream.toByteArray(), "UTF-8");
//
//                                if(str.endsWith(System.getProperty("line.seperator"))) {
//                                    System.out.println(">> " + str);
//                                }
//                            }
//                        });

//                        channel.setErr((System.err));
//                        channel.setIn(new NoCloseInputStream(System.in));
//                        channel.setOut(new NoCloseOutputStream(System.out));
//                        channel.setErr(new NoCloseOutputStream(System.err));

                        channel.open();

                        channel.waitFor(Arrays.asList(new ClientChannelEvent[]{ClientChannelEvent.CLOSED}), 0);
                    } finally {
                        session.close(false);
                    }
                } finally {
                    client.stop();
                }
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static void main_server(String[] args) {
        try {
            SshServer sshd = SshServer.setUpDefaultServer();

            sshd.setPort(2222);

            File keyPairFile = new File("./keypair.ser");

            if(!keyPairFile.exists()) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

                KeyPair keyPair = generator.generateKeyPair();

                ObjectOutputStream outs = new ObjectOutputStream(new FileOutputStream(keyPairFile));

                outs.writeObject(keyPair);

                outs.flush();

                outs.close();
            }

            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyPairFile));

            sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/bash", "-i", "-l" }));

            CommandFactory myCommandFactory = new CommandFactory() {
                public Command createCommand(String s) {
                    logger.info("[myCommandFactory.createCommand] s : {}", s);

                    Command cmd = new Command() {
                        private InputStream ins;
                        private OutputStream outs;
                        private OutputStream errs;
                        private ExitCallback exitCallback;

                        public void setInputStream(InputStream inputStream) {
                            this.ins = inputStream;
                        }

                        public void setOutputStream(OutputStream outputStream) {
                            this.outs = outputStream;
                        }

                        public void setErrorStream(OutputStream outputStream) {
                            this.errs = outputStream;
                        }

                        public void setExitCallback(ExitCallback exitCallback) {
                            this.exitCallback = exitCallback;
                        }

                        public void start(Environment environment) throws IOException {
                            logger.info("[Command.start] environment : {}", environment);
                            logger.info("[Command.start] exitCallback : {}", this.exitCallback);

                            this.outs.write(("测试" + System.getProperty("line.separator")).getBytes("UTF-8"));
                            this.outs.flush();

                            (new Thread(() -> {
                                try {
                                    byte[] buffer = new byte[10];

                                    int len = -1;

                                    while(-1 != (len = this.ins.read(buffer))) {
                                        System.out.write(buffer, 0, len);
                                    }

                                    exitCallback.onExit(0);
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                            })).start();

//                            (new Thread(() -> {
//                                try {
//                                    byte[] buffer = new byte[10];
//
//                                    int len = -1;
//
//                                    while(-1 != (len = System.in.read(buffer))) {
//                                        this.outs.write(buffer, 0, len);
//                                    }
//                                } catch (Exception e) {
//                                    logger.error(e.getMessage(), e);
//                                }
//                            })).start();
                        }

                        public void destroy() throws Exception {
                            logger.info("[Command.destroy]");
                        }
                    };

                    return cmd;
                }
            };

            ScpCommandFactory scpCommandFactory = new ScpCommandFactory();

            scpCommandFactory.setDelegateCommandFactory(myCommandFactory);

            sshd.setCommandFactory(scpCommandFactory);

            sshd.setForwarderFactory(new DefaultForwarderFactory());

            sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
                public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) {
                    logger.info("[PublickeyAuthenticator.authenticate] s : {}, publicKey: {}, serverSession : {}", s, publicKey, serverSession);
                    return true;
                }
            });

            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
                public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException {
                    logger.info("[PasswordAuthenticator.authenticate]s : {}, s1: {}, serverSession : {}", s, s1, serverSession);
                    return true;
                }
            });

            sshd.start();

            System.out.println("Press any key to stop...");

            System.in.read();

            sshd.close();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
