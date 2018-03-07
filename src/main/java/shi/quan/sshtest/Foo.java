package shi.quan.sshtest;

import org.springframework.stereotype.Component;

@Component
public class Foo {
    public String bar(String in) {
        return "Foo Bar " + in;
    }
}
