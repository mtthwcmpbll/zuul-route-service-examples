package io.pivotal.examples.routing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application
{
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${greeting}")
    private String greeting;


    public static void main( String[] args )
    {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping("/")
    public String hello() {
        StringBuilder str = new StringBuilder();

        //Greeting
        str.append(greeting+", world!\n");

        // parse the VCAP_APPLICATION environment variable to give details on the environment
        String envJsonStr = System.getenv("VCAP_APPLICATION");
        if (null != envJsonStr) {
            logger.info("Found VCAP_APPLICATION:  " + envJsonStr);

            JsonObject envJson = new JsonParser().parse(envJsonStr).getAsJsonObject();
            String cfApi = envJson.getAsJsonPrimitive("cf_api").getAsString();
            str.append("Running in foundation:  " + cfApi);
        } else {
            str.append("Not running in a PCF foundation.");
        }

        return str.toString();
    }
}
