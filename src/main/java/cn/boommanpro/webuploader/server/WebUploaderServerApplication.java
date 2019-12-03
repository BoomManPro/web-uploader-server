package cn.boommanpro.webuploader.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import cn.boommanpro.webuploader.server.config.UploaderConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * WebUploaderServer启动类
 *
 * @author wangqimeng
 * @date 2019/9/20 16:59
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(UploaderConfigProperties.class)
public class WebUploaderServerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(WebUploaderServerApplication.class, args);
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            TomcatServletWebServerFactory tomcatServletWebServerFactory = (TomcatServletWebServerFactory) context.getBean("tomcatServletWebServerFactory");
            int port = tomcatServletWebServerFactory.getPort();
            String contextPath = tomcatServletWebServerFactory.getContextPath();
            log.info("<------------------------------------------ http://{}:{}{}/ ------------------------------------------>", host, port, contextPath);
        } catch (UnknownHostException e) {
            log.error("项目启动异常", e);
        }
        log.info("{}系统启动成功", WebUploaderServerApplication.class.getSimpleName());
    }

}
