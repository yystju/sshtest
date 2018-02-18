package shi.quan.sshtest;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.forward.DefaultForwarderFactory;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Service
public class SSHClient {

    private static Logger logger = LoggerFactory.getLogger(SSHClient.class);

    public void open() {
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

                ConnectFuture connectFuture = client.connect("root", "localhost", 2222);

                connectFuture.await();

                try(ClientSession session = connectFuture.getSession()) {
                    session.addPasswordIdentity("passw0rd");

                    session.auth().verify(10000);

                    SshdSocketAddress localAddress = new SshdSocketAddress("localhost", 4444);
                    SshdSocketAddress remoteAddress = new SshdSocketAddress("localhost", 2222);

                    try (ExplicitPortForwardingTracker localTracker = session.createLocalPortForwardingTracker(localAddress, remoteAddress)) {
                        boolean isLocalForwarding = localTracker.isLocalForwarding();
                    } finally {
                        session.close();
                    }

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
                } finally {
                    client.stop();
                }
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
