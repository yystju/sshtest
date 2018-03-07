package commands

import org.crsh.cli.Argument
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.command.InvocationContext
import org.springframework.beans.factory.BeanFactory
import shi.quan.sshtest.Foo

class hello {
    @Usage("Hello")
    @Command
    def main(InvocationContext context) {
        return "hello"
    }

    @Usage('Say Hello')
    @Command
    def say(
            InvocationContext context,
            @Usage('The message') @Argument String message
    ) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        Foo foo = beans.getBean(Foo)

        return foo.bar(message)
    }
}
