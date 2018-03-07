package shi.quan.sshtest;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.forward.DefaultForwarderFactory;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
public class SSHClient {

    private static Logger logger = LoggerFactory.getLogger(SSHClient.class);

    public void shell(String userName, String password, String sshServerHost, int sshServerPort) {
        try {
            try(SshClient client = SshClient.setUpDefaultClient()) {
                client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
                client.start();

                client.setForwardingFilter(new AcceptAllForwardingFilter());
                client.setForwarderFactory(new DefaultForwarderFactory());

//                client.setFilePasswordProvider(new FilePasswordProvider() {
//                    @Override
//                    public String getPassword(String s) throws IOException {
//                        logger.info("s : {}", s);
//                        return "passwd";
//                    }
//                });

                ConnectFuture connectFuture = client.connect(userName, sshServerHost, sshServerPort);

                connectFuture.await();

                try(ClientSession session = connectFuture.getSession()) {
                    session.addPasswordIdentity(password);

                    session.auth().verify(10000);

                    try(ChannelShell channel = session.createShellChannel()) {
                        logger.info("channel.getPtyType() : {}", channel.getPtyType());
                        logger.info("channel.getPtyModes() : {}", channel.getPtyModes());

                        channel.setPtyType("xterm-256color");

                        Map<PtyMode, Integer> modes = channel.getPtyModes();

                        for(PtyMode key : modes.keySet()) {
                            if(key == PtyMode.ECHO) {
                                modes.put(key, 0);
                            } else if(key == PtyMode.ECHOCTL) {
                                modes.put(key, 0);
                            } else if(key == PtyMode.ECHOE) {
                                modes.put(key, 0);
                            } else if(key == PtyMode.ECHOK) {
                                modes.put(key, 0);
                            } else if(key == PtyMode.ECHOKE) {
                                modes.put(key, 0);
                            } else if(key == PtyMode.ECHONL) {
                                modes.put(key, 0);
                            }
                        }

                        channel.setPtyModes(modes);

                        channel.setIn(new NoCloseInputStream(System.in));
                        channel.setOut(new NoCloseOutputStream(System.out));
                        channel.setErr(new NoCloseOutputStream(System.err));

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

    public void l(String userName, String password, String sshServerHost, int sshServerPort, String localHost, int localPort, String remoteHost, int remotePort) {
        try {
            try(SshClient client = SshClient.setUpDefaultClient()) {
                client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
                client.start();

                client.setForwardingFilter(new AcceptAllForwardingFilter());
                client.setForwarderFactory(new DefaultForwarderFactory());

                ConnectFuture connectFuture = client.connect(userName, sshServerHost, sshServerPort);

                connectFuture.await();

                try(ClientSession session = connectFuture.getSession()) {
                    session.addPasswordIdentity(password);

                    session.auth().verify(10000);

                    SshdSocketAddress localAddress = new SshdSocketAddress(localHost, localPort);
                    SshdSocketAddress remoteAddress = new SshdSocketAddress(remoteHost, remotePort);

                    try (ExplicitPortForwardingTracker remoteTracker = session.createLocalPortForwardingTracker(localAddress, remoteAddress)) {
                        System.out.println("Press any key...");
                        System.in.read();
                    } finally {
                        session.close();
                    }
                } finally {
                    client.stop();
                }
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void r(String userName, String password, String sshServerHost, int sshServerPort, String remoteHost, int remotePort, String localHost, int localPort) {
        try {
            try(SshClient client = SshClient.setUpDefaultClient()) {
                client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
                client.start();

                client.setForwardingFilter(new AcceptAllForwardingFilter());
                client.setForwarderFactory(new DefaultForwarderFactory());

                ConnectFuture connectFuture = client.connect(userName, sshServerHost, sshServerPort);

                connectFuture.await();

                try(ClientSession session = connectFuture.getSession()) {
                    session.addPasswordIdentity(password);

                    session.auth().verify(10000);

                    SshdSocketAddress localAddress = new SshdSocketAddress(localHost, localPort);
                    SshdSocketAddress remoteAddress = new SshdSocketAddress(remoteHost, remotePort);

                    try (ExplicitPortForwardingTracker localTracker = session.createRemotePortForwardingTracker(remoteAddress, localAddress)) {
                        System.out.println("Press any key...");
                        System.in.read();
                    } finally {
                        session.close();
                    }
                } finally {
                    client.stop();
                }
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
