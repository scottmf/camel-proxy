package com.vmware.blackpearl.proxy;

import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
@ComponentScan
public class CamelServletJmsSampleApplication extends SpringBootServletInitializer {

    public static void main(String[] args) throws Exception {
        Properties systemProps = System.getProperties();
//        systemProps.put("javax.net.debug","ssl");
        System.setProperties(systemProps);

        SpringApplication.run(CamelServletJmsSampleApplication.class, args);
    }
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        builder.sources(CamelServletJmsSampleApplication.class);
        return super.configure(builder);
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadFactory(new ThreadFactory() {
            private final AtomicLong threadNum = new AtomicLong(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "httpExecutor-" + threadNum.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
        threadPoolTaskScheduler.setPoolSize(16);
        return threadPoolTaskScheduler;
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        CamelHttpTransportServlet servlet = new CamelHttpTransportServlet();
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(servlet, "/*");
        servletRegistrationBean.setName("CamelServlet");
        return servletRegistrationBean;
    }

/*
 * NOTE: need to create a keystore for this to work
 * keytool -genkey -alias apache -keyalg RSA -sigalg SHA1WithRSA -keysize 2048 -keystore ${HOME}/proxy.keystore -dname "CN=www.oaklandathletics.com,OU=it, O=As, L=Oakland, ST=CA, C=US" -storepass cmpkeystore
    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() throws Exception {
        return new EmbeddedServletContainerCustomizer () {
            @Override
             public void customize(ConfigurableEmbeddedServletContainer factory) {
                 if (factory instanceof TomcatEmbeddedServletContainerFactory) {
                     TomcatEmbeddedServletContainerFactory containerFactory = (TomcatEmbeddedServletContainerFactory) factory;
                     AccessLogValve accessLogValve = new AccessLogValve();
                     accessLogValve.setPattern("remoteIp='%h' %l %u %t call='%r' returnCode='%s' bytes='%b' " +
                         "duration='%D' auth='%{AUTHORIZATION}i' traceId='%{trace-id}i'");
                     String dir = System.getProperty("user.dir");
                     accessLogValve.setDirectory(dir + File.separator + "logs" + File.separator);
                     accessLogValve.setPrefix("access_log-");
                     accessLogValve.setSuffix(".txt");
                     containerFactory.addContextValves(accessLogValve);
                     containerFactory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
                             @Override
                             public void customize(Connector connector) {
                                 connector.setPort(8443);
                                 connector.setSecure(true);
                                 connector.setScheme("https");
                                 Http11NioProtocol proto = (Http11NioProtocol) connector.getProtocolHandler();
                                 proto.setSSLEnabled(true);
                                 proto.setKeystoreFile(getProxyKeyStore());
                                 proto.setKeystorePass("cmpkeystore");
                                 proto.setKeyAlias("apache");
                                 proto.setKeyPass("cmpkeystore");
                             }
                         });
                 }
             }
        };
    }

    static final String getProxyKeyStore() {
        String home = System.getProperty("user.home");
        String fs = File.separator;
        return home + fs + "proxy.keystore";
    }
*/
}
