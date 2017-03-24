package io.pivotal.examples.routing;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            LOG.info("CF route service received a request for " + forwardedUrl);
            try {
                requestContext.setRouteHost(new URL(forwardedUrl));
            } catch (MalformedURLException exc) {
                LOG.error("Malformed URL in " + FORWARDED_URL + ":  " + forwardedUrl);
            }
        } else {
            LOG.info("CF route service received a request without a X-CF-Forwarded-Url");
        }

        return null;
    }
}