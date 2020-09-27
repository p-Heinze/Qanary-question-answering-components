package writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DockerComposeWriter {

    private String dockerComposeFilePath;
    private String qanaryProjectPomLocation;
    private String qanaryPath;

    public DockerComposeWriter() {
        try {
            File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            this.qanaryPath = jarFile.getParentFile().getParentFile().getParentFile().getCanonicalPath();
            this.dockerComposeFilePath = this.qanaryPath+"/docker-compose.yml";
            this.qanaryProjectPomLocation = this.qanaryPath+"/pom.xml";

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public void createDockerComposeFile() {
        try {
            File file = new File(this.dockerComposeFilePath);
            if (file.createNewFile()) {
                System.out.println("created compose file: " + this.dockerComposeFilePath);
            } else {
                System.out.println("file already exists: " + this.dockerComposeFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter writer = new FileWriter(this.dockerComposeFilePath);
            String head = "version: '3'\nservices:\n";
            writer.write(head);
            writer.close();
            this.iterateComponents();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Map<String, String> addPort(Map<String, String> componentAtrr, String componentPropertiesFilePath) {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(componentPropertiesFilePath));
            String port = properties.get("server.port").toString();
            componentAtrr.put("port", port);
            return componentAtrr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> addPropertiesFromPom(Map<String, String> componentAttr, String componentPomFilePath) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        try {
            Document document = builder.parse(new FileInputStream(componentPomFilePath));
            Element root = document.getDocumentElement();
            Node versionNode = root.getElementsByTagName("version").item(0);
            Node dockerImageNameNode = ((Element)root.getElementsByTagName("properties").item(0)).getElementsByTagName("docker.image.name").item(0);
            String version = versionNode.getTextContent();
            String dockerImageName = dockerImageNameNode.getTextContent();
            componentAttr.put("image", dockerImageName);
            componentAttr.put("version", version);
            return componentAttr;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void iterateComponents() {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        try {
            Document document = builder.parse(new FileInputStream(this.qanaryProjectPomLocation));
            Element root = document.getDocumentElement();
            Element profiles = (Element)root.getElementsByTagName("profiles").item(0);
            Element modules = (Element)profiles.getElementsByTagName("modules").item(0);
            NodeList components = modules.getElementsByTagName("module");
            int basePort = 10000;

            for(int i = 0; i < components.getLength(); ++i) {
                String component = components.item(i).getTextContent();
                System.out.println(component);
                Map<String, String> componentAttr = new HashMap();
                String componentPomFilePath = this.qanaryPath+"/"+component+ "/pom.xml";
                String componentPropertiesFilePath = this.qanaryPath+"/"+component+ "/src/main/resources/config/application.properties";
                componentAttr = this.addPropertiesFromPom(componentAttr, componentPomFilePath);
                componentAttr = this.addPort(componentAttr, componentPropertiesFilePath);
                componentAttr.put("newPort", basePort+i+"");
                this.appendToDockerComposeFile(componentAttr);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

    }

    private void appendToDockerComposeFile(Map<String, String> componentAttr) {
        try {
            FileWriter writer = new FileWriter(this.dockerComposeFilePath, true);
            String version = componentAttr.get("version");
            String image = componentAttr.get("image");
            String port = componentAttr.get("port");
            String newPort = componentAttr.get("newPort");
            String dockerCompose = "" +
                    "  " + image + ":\n" +
                    "    image: " + image + ":" + version + "\n" +
                    "    ports: \n" +
                    "      - \"" + newPort + ":" + port + "\"\n";
            writer.write(dockerCompose);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
