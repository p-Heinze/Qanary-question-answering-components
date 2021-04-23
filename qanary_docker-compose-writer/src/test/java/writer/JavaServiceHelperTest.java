package writer;

import org.junit.jupiter.api.Test;

import writer.except.DockerComposeWriterException;
import writer.helpers.JavaServiceHelper;

import static org.junit.Assert.*;

import java.nio.file.Paths;

class JavaServiceHelperTest {

    @Test
    void testGetServiceSectionAsString() {
        // STANDARD USE CASE

        // given minimal parameters
        String name = "test-service";
        String version = "0.1.0";
        String directory = "./qa-test-service";

        // and custom settings
        Long port = 5050L;
        String imagePrefix = "user/";
        String pipelineEndpoint = "http://localhost:8080";

        // hold the information
        ComponentInformation service = new ComponentInformation(name, version, name);
        service.setImagePrefix(imagePrefix);
        service.setPipelineEndpoint(pipelineEndpoint);
        service.setPort(port);

        assertNotNull(service.getPort());
        assertNotNull(service.getPipelineEndpoint());
        assertNotNull(service.getImagePrefix());

        // instantiate java writer to create service section
        JavaServiceHelper javaServiceHelper = new JavaServiceHelper(directory);
        String expected = "" +
                "  "+name + ":\n" +
                "    image: " + imagePrefix + name + ":" + version + "\n" +
                "    ports:\n" +
                "      - \""+port+":"+port+"\"\n" +
                "    restart: unless-stopped\n" +
                "    environment: \n" +
                "      - \"SERVER_PORT="+port+"\"\n" +
                "      - \"SPRING_BOOT_ADMIN_URL="+pipelineEndpoint+"\"\n";

        String actual = javaServiceHelper.getServiceSectionAsString(service);

        assertEquals(expected,actual);
    }        

    @Test
    void testGetServiceCoinfiguration() throws DockerComposeWriterException {

        String abs = Paths.get("../").toAbsolutePath().normalize().toString();
        System.out.println(abs);
        String javaDirectory= abs+"/qanary_component-NED-DBpedia-Spotlight";

        JavaServiceHelper helper = new JavaServiceHelper(javaDirectory);
        ComponentInformation information = helper.getServiceConfiguration();

        assertEquals("qanary_component-ned-dbpedia-spotlight", information.getServiceName());
        assertEquals("qanary_component-ned-dbpedia-spotlight", information.getImageName());
        assertNotNull(information.getServiceVersion());
    }
}
        
