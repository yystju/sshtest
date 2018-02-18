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

//    @Bean
//    CommandLineRunner server() {
//        return (args) -> {
//            sshService.start();
//
//            System.out.println("Press any key...");
//            System.in.read();
//
//
//            sshService.shutdown();
//        };
//    }

    @Bean
    CommandLineRunner client() {
        return (args) -> {
            sshClient.open();
        };
    }
}
