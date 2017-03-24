package io.pivotal.examples.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@EnableZuulProxy
@SpringBootApplication
public class RouteServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteServicesApplication.class, args);
    }

}