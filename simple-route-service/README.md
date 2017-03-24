# Using Zuul as a CF Route service

CloudFoundry provides a way to bind an app running in the platform as a special
kind of service called a _route service_.  Rather than binding an application to
the service instance like normal, you bind an applications _route_ to that service.
You won't see any additional environment variables injected into your app with
`cv env`, and for all intents and purposes the app itself is unaware of the routing
magic the platform is performing behind the scenes.

Ultiamtely, the goal of a route service is to provide a way to slide in cross-cutting
functionality like the stuff mentioned previously without forcing developers
to explicitly include that feature in each app.  With a route service, you can
bind your app and any functionality performed by the service on the request is
done before it ever hits the app.

An implementation of a route service in CF is pretty simple as the gorouter handles
most of the complexity of sending requests where they need to go (redirecting requests
to a bound app through the route service and then back through the gorouter to
reach their original destination).  The contract for a route service basically says
"Respect the `X-CF-Forwarded-Url` header, which is used to tell the gorouter where
the request intends to go.  Leave the other two headers (TODO) alone, as they're used
by the platform during routing".  Below is an implementation of a Zuul filter that
allows a ZuulProxy application to act as a route service:

``` java
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
class CfRouteServiceFilter extends ZuulFilter {
    private static Logger LOG = LoggerFactory.getLogger(CfRouteServiceFilter.class);

    static final String FORWARDED_URL = "X-CF-Forwarded-Url";

    @Override
    public String filterType() {
        return "route";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext();

        // find the X-CF-Forwarded-Url header to get the path originally requested
        String forwardedUrl = requestContext.getRequest().getHeader(FORWARDED_URL);
        if (null != forwardedUrl) {
            try {
                requestContext.setRouteHost(new URL(forwardedUrl));
            } catch (MalformedURLException exc) {
                LOG.error("Malformed URL in " + FORWARDED_URL + ":  " + forwardedUrl);
            }
        }

        return null;
    }
}
```

This can be run in a project with the basic Spring Boot application:
``` java
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
```

And an `application.yml` with the bare minimum Zuul configuration:
``` yaml
zuul:
  ignoredServices: '*'
  routes:
    passthrough:
      path: /**
```

In order to use this app as a route service, you'll need to:
 
1. build it using `mvn clean package`
2. Push the route service app to CloudFoundry with `cf push simple-route-service -p target/simple-route-service-1.0.0-SNAPSHOT.jar`
3. You can check the route for the app once it's up with `cf apps`:
``` bash
Getting apps in org pcfdev-org / space route-service-examples as user...
OK

name                   requested state   instances   memory   disk   urls
hello                  started           1/1         256M     512M   hello.local.pcfdev.io
simple-route-service   started           1/1         256M     512M   simple-route-service.local.pcfdev.io
```
4. expose it as a user-provided service with `cf create-user-provided-service simple-route-svc -r https://simple-route-service.local.pcfdev.io`
5. bind it to an app as a route service with `cf bind-route-service local.pcfdev.io simple-route-svc --hostname hello`

You now have an custom route service bound to the route `hello.local.pcfdev.io` (or to whatever app and route you used
in that final command).  You can test this by tailing the logs to your route service app (`cf logs simple-route-service`)
and hitting the route your bound to it (in this case, the `hello` app).

``` bash
Connected, tailing logs for app simple-route-service in org pcfdev-org / space route-service-examples as user...

2017-03-24T10:33:25.37-0400 [APP/PROC/WEB/0]OUT 2017-03-24 14:33:25.373  INFO 13 --- [nio-8080-exec-1] i.p.e.routing.CfRouteServiceFilter       : CF route service received a request for https://hello.local.pcfdev.io/
2017-03-24T10:33:25.51-0400 [RTR/0]      OUT simple-route-service.local.pcfdev.io - [24/03/2017:14:33:25.370 +0000] "GET / HTTP/1.1" 200 0 64 "-" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36" 192.168.11.11:35203 10.0.2.15:60048 x_forwarded_for:"192.168.11.1" x_forwarded_proto:"http" vcap_request_id:3b741618-c034-47aa-6155-2439cd601bfa response_time:0.142576802 app_id:f319e36b-319f-4eaa-a8e7-3609a69a89c6 app_index:0
```
