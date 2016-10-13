package net.technolords.micro.config.jaxb;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import net.technolords.micro.config.jaxb.namespace.NamespaceList;
import net.technolords.micro.config.jaxb.resource.ResourceGroups;
import net.technolords.micro.config.jaxb.resource.SimpleResource;

public class Configuration {
    private String type;
    private String url;
    private SimpleResource simpleResource;
    private ResourceGroups resourceGroups;
    private NamespaceList namespaceList;
    private Map<String, String> cachedNamespaceMapping;

    @XmlAttribute(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name = "url")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @XmlElement(name = "resource")
    public SimpleResource getSimpleResource() {
        return simpleResource;
    }

    public void setSimpleResource(SimpleResource simpleResource) {
        this.simpleResource = simpleResource;
    }

    @XmlElement(name = "resource-groups")
    public ResourceGroups getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(ResourceGroups resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    @XmlElement(name = "namespaces")
    public NamespaceList getNamespaceList() {
        return namespaceList;
    }

    public void setNamespaceList(NamespaceList namespaceList) {
        this.namespaceList = namespaceList;
    }

    @XmlTransient
    public Map<String, String> getCachedNamespaceMapping() {
        return cachedNamespaceMapping;
    }

    public void setCachedNamespaceMapping(Map<String, String> cachedNamespaceMapping) {
        this.cachedNamespaceMapping = cachedNamespaceMapping;
    }
}
