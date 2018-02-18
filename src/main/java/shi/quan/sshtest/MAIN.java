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

    @Autowired
    SSHClient sshClient;

    @Bean
    CommandLineRunner server() {
        return (args) -> {
            if(args.length > 0 && "server".equals(args[0])) {

                sshService.start();

                System.out.println("Press any key...");
                System.in.read();


                sshService.shutdown();
            } else if (args.length >= 5 && "client".equals(args[0])) {
                sshClient.shell(args[1], args[2], args[3], Integer.parseInt(args[4]));
            } else if (args.length >= 9 && "local".equals(args[0])) {
                sshClient.l(args[1], args[2], args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
            } else if (args.length >= 9 && "remote".equals(args[0])) {
                sshClient.r(args[1], args[2], args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
            } else {
                System.out.println("Usage java -jar sshtest.jar [server|client|local|remote]");
                System.out.println("\tserver -- No parameters.");
                System.out.println("\tclient -- [username] [password] [hostname] [port]");
                System.out.println("\tlocal -- [username] [password] [hostname] [port] [localhost] [localport] [remotehost] [remoteport]");
                System.out.println("\tremote -- [username] [password] [hostname] [port] [remotehost] [remoteport] [localhost] [localport]");
            }
        };
    }
}
