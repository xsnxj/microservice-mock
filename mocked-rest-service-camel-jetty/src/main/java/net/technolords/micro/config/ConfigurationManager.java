package net.technolords.micro.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import net.technolords.micro.config.jaxb.Configuration;
import net.technolords.micro.config.jaxb.Configurations;
import net.technolords.micro.config.jaxb.resource.ResourceGroup;
import net.technolords.micro.config.jaxb.resource.ResourceGroups;
import net.technolords.micro.config.jaxb.resource.SimpleResource;

/**
 * Created by Technolords on 2016-Jul-20.
 */
public class ConfigurationManager {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    public static final String HTTP_POST = "POST";
    private static final String PATH_TO_CONFIG_FILE = "xml/configuration.xml";
    private static final String PATH_TO_SCHEMA_FILE = "xsd/configurations.xsd";
    private Configurations configurations = null;
    private XpathEvaluator xpathEvaluator = null;
    private Map<String, Configuration> getConfigurations = new HashMap<>();
    private Map<String, Configuration> postConfigurations = new HashMap<>();

    public ConfigurationManager(String pathToConfig) throws JAXBException, IOException, SAXException {
        if (pathToConfig == null || pathToConfig.isEmpty()) {
            pathToConfig = PATH_TO_CONFIG_FILE;
        }
        pathToConfig = PATH_TO_CONFIG_FILE; // TODO: remove later
        // Validate configuration file
        this.validateConfigurationFile(pathToConfig);
        // Initialize configuration
        this.initializeConfiguration(pathToConfig);
    }

    /**
     * Auxiliary method to find a response for a given get request, based on the path.
     *
     * @param path
     *  The path associated with the get request.
     * @return
     *  The response associated with the get request.
     *
     * @throws JAXBException
     *  When reading the configuration fails.
     */
    public String findResponseForGetOperationWithPath(String path) throws JAXBException, IOException, InterruptedException {
        LOGGER.debug("About to find response for get operation with path: {}", path);
        if (this.getConfigurations.containsKey(path)) {
            LOGGER.debug("... found, proceeding to the data part...");
            Configuration configuration = this.getConfigurations.get(path);
            SimpleResource resource = configuration.getSimpleResource();
            // Load and update cache
            LOGGER.debug("About to load data from: {}", resource.getResource());
            return this.readResourceCacheOrFile(resource);
        }
        LOGGER.debug("... not found!");
        return null;
    }

    /**
     * Auxiliary method to find a response for a given post request, based on the path and the message (body).
     *
     * @param path
     *  The path associated with the post request.
     * @param message
     *  The message associated with the post request.
     *
     * @return
     *  The response associated with the post request.
     *
     * @throws IOException
     *  When reading the response fails.
     * @throws XPathExpressionException
     *  When evaluation the xpath expression fails.
     * @throws JAXBException
     *  When reading the configuration fails.
     */
    public String findResponseForPostOperationWithPathAndMessage(String path, String message) throws IOException, XPathExpressionException, JAXBException, InterruptedException {
        LOGGER.debug("About to find response for post operation with path: {}", path);
        if (this.postConfigurations.containsKey(path)) {
            LOGGER.debug("... found, proceeding to the data part...");
            Configuration configuration = this.postConfigurations.get(path);
            // Iterate the resources, and verify whether the xpath matches with the data
            ResourceGroups resourceGroups = configuration.getResourceGroups();
            for (ResourceGroup resourceGroup : resourceGroups.getResourceGroup()) {
                if (resourceGroup.getXpathConfig() != null) {
                    LOGGER.debug("... found xpath: {}", resourceGroup.getXpathConfig().getXpath());
                    if (this.xpathEvaluator.evaluateXpathExpression(resourceGroup.getXpathConfig().getXpath(), message, configuration)) {
                        LOGGER.debug("... xpath matched, about to find associated resource");
                        return this.readResourceCacheOrFile(resourceGroup.getSimpleResource());
                    }
                } else {
                    LOGGER.debug("No xpath configured, about to load the data from: {}", resourceGroup.getSimpleResource().getResource());
                    return this.readResourceCacheOrFile(resourceGroup.getSimpleResource());
                }
            }
        }
        LOGGER.debug("... not found!");
        return null;
    }

    /**
     * Auxiliary method to initialize the configuration manager, which means:
     * - reading and parsing the xml configuration
     * - instantiating a xpath evaluator
     *
     * @param pathToConfig
     *  The path to the configuration file.
     *
     * @throws JAXBException
     *  When parsing the XML configuration file fails.
     */
    protected void initializeConfiguration(String pathToConfig) throws JAXBException {
        LOGGER.info("About to initialize the configuration...");
        // TODO: path to config
        Unmarshaller unmarshaller = JAXBContext.newInstance(Configurations.class).createUnmarshaller();
        this.configurations = (Configurations) unmarshaller.unmarshal(this.getClass().getClassLoader().getResourceAsStream(pathToConfig));
        LOGGER.debug("Total loaded resources: {}", this.configurations.getConfigurations().size());
        for (Configuration configuration : this.configurations.getConfigurations()) {
            if (HTTP_POST.equals(configuration.getType().toUpperCase())) {
                // Add resource to post configuration group
                this.postConfigurations.put(configuration.getUrl(), configuration);
            } else {
                // Add resource to get configuration group
                this.getConfigurations.put(configuration.getUrl(), configuration);
            }
        }
        LOGGER.info("... done, URL mappings parsed [{} for POST, {} for GET]", this.postConfigurations.size(), this.getConfigurations.size());
        this.xpathEvaluator = new XpathEvaluator();
    }

    private void validateConfigurationFile(String pathToConfig) throws IOException, SAXException {
        LOGGER.info("About to validate the configuration...");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source xsdSource = new StreamSource(this.getClass().getClassLoader().getResourceAsStream(PATH_TO_SCHEMA_FILE));
        Schema schema = schemaFactory.newSchema(xsdSource);
        Validator validator = schema.newValidator();
        Source sourceToConfig = new StreamSource(this.getClass().getClassLoader().getResourceAsStream(pathToConfig));
        validator.validate(sourceToConfig);
        LOGGER.info("... valid, proceeding...");
    }

    /**
     * Auxiliary method that reads the response data as well as updating the internal cache so
     * subsequent reads will will served from memory.
     *
     * @param resource
     *  The resource to read and cache.
     *
     * @return
     *  The data associated with the resource (i.e. response).
     *
     * @throws IOException
     *  When reading the resource fails.
     */
    private String readResourceCacheOrFile(SimpleResource resource) throws IOException, InterruptedException {
        if (resource.getDelay() > 0) {
            LOGGER.debug("About to delay {} ms", resource.getDelay());
            Thread.sleep(resource.getDelay());
        }
        if (resource.getCachedData() != null) {
            return resource.getCachedData();
        }
        InputStream fileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource.getResource());
        LOGGER.debug("Path to file exists: {}",  fileStream.available());
        resource.setCachedData(new BufferedReader(new InputStreamReader(fileStream)).lines().collect(Collectors.joining("\n")));
        return resource.getCachedData();
    }

}
