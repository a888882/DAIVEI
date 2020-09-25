/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.pom;

import com.microsoft.azure.common.utils.IndentUtil;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.springcloud.SpringCloudAppConfig;
import com.microsoft.azure.springcloud.SpringCloudAppDeploymentConfig;
import com.microsoft.azure.springcloud.utils.ResourcesUtils;
import com.microsoft.azure.springcloud.utils.XmlUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PomXmlUpdater {
    private MavenProject project;
    private PluginDescriptor plugin;

    public PomXmlUpdater(MavenProject project, PluginDescriptor plugin) {
        this.project = project;
        this.plugin = plugin;
    }

    public void updateSettings(SpringCloudAppConfig app, SpringCloudAppDeploymentConfig deploy) throws DocumentException, IOException {
        final File pom = this.project.getFile();
        final SAXReader reader = new CustomSAXReader();
        reader.setDocumentFactory(new LocatorAwareDocumentFactory());
        final Document doc = reader.read(new InputStreamReader(new FileInputStream(pom)));
        final Element pluginsNode = createToPath(doc.getRootElement(), "build", "plugins");
        Element springPluginNode = null;
        for (final Element element : pluginsNode.elements()) {
            final String groupId = XmlUtils.getChildValue(element, "groupId");
            final String artifactId = XmlUtils.getChildValue(element, "artifactId");
            final String version = XmlUtils.getChildValue(element, "version");
            if (plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId) &&
                (version == null || StringUtils.equals(plugin.getVersion(), version))) {
                springPluginNode = element;
            }
        }
        if (springPluginNode == null) {
            springPluginNode = createMavenSpringPluginNode(pluginsNode);
        }

        Element newNode;
        if (app != null || deploy != null) {
            final Element configurationNode = createToPath(springPluginNode, "configuration");
            if (app != null) {
                PomXmlUpdater.applyToDom4j(app, configurationNode);
            }
            if (deploy != null) {
                final Element deployNode = createToPath(configurationNode, "deployment");
                PomXmlUpdater.applyToDom4j(deploy, deployNode);
                ResourcesUtils.applyDefaultResourcesToDom4j(deployNode);
            }
            // use configurationNode as initial value since we want to format configuration node if it exists.
            newNode = configurationNode;
        } else {
            newNode = springPluginNode;
        }

        // newly created nodes are not LocationAwareElement
        while (!(newNode.getParent() instanceof LocationAwareElement)) {
            newNode = newNode.getParent();
        }
        FileUtils.fileWrite(pom, formatElement(FileUtils.fileRead(pom), (LocationAwareElement) newNode.getParent(), newNode));
    }

    private static String formatElement(String originalXml, LocationAwareElement parent, Element newNode) {
        final String[] originXmlLines = TextUtils.splitLines(originalXml);
        final String baseIndent = IndentUtil.calcXmlIndent(originXmlLines, parent.getLineNumber() - 1,
            parent.getColumnNumber() - 2);
        final String placeHolder = String.format("@PLACEHOLDER_RANDOM_%s@", RandomUtils.nextLong());
        final Text placeHolderNode = new DefaultText("\n" + placeHolder);
        // replace target node to placeholder
        parent.content().replaceAll(t -> t == newNode ? placeHolderNode : t);
        newNode.setParent(null);
        // remove all spaces before target node
        XmlUtils.trimTextBeforeEnd(parent, placeHolderNode);
        final String xmlWithPlaceholder = parent.getDocument().asXML();

        final String[] newXmlLines = TextUtils.splitLines(XmlUtils.prettyPrintElementNoNamespace(newNode));
        final String replacement = Arrays.stream(newXmlLines).map(t -> baseIndent + "    " + t).collect(Collectors.joining("\n")) + "\n" + baseIndent;
        return xmlWithPlaceholder.replace(placeHolder, replacement);
    }

    private Element createMavenSpringPluginNode(Element pluginsRootNode) {
        final Element result = new DOMElement("plugin");
        XmlUtils.addDomWithKeyValue(result, "groupId", this.plugin.getGroupId());
        XmlUtils.addDomWithKeyValue(result, "artifactId", this.plugin.getArtifactId());
        XmlUtils.addDomWithKeyValue(result, "version", this.plugin.getVersion());
        pluginsRootNode.add(result);
        return result;
    }

    private static Element createToPath(Element node, String... paths) {
        for (final String path : paths) {
            Element newNode = node.element(path);
            if (newNode == null) {
                newNode = new DOMElement(path);
                node.add(newNode);
            }
            node = newNode;
        }
        return node;
    }

    // code copied from https://stackoverflow.com/questions/36006819/use-dom4j-to-locate-the-node-with-line-number
    static class CustomSAXReader extends SAXReader {
        @Override
        protected SAXContentHandler createContentHandler(XMLReader reader) {
            return new CustomSAXContentHandler(getDocumentFactory(), getDispatchHandler());
        }

        @Override
        public void setDocumentFactory(DocumentFactory documentFactory) {
            super.setDocumentFactory(documentFactory);
        }

    }

    static class CustomSAXContentHandler extends SAXContentHandler {
        // this is already in SAXContentHandler, but private
        private DocumentFactory documentFactory;

        public CustomSAXContentHandler(DocumentFactory documentFactory, ElementHandler elementHandler) {
            super(documentFactory, elementHandler);
            this.documentFactory = documentFactory;
        }

        @Override
        public void setDocumentLocator(Locator documentLocator) {
            super.setDocumentLocator(documentLocator);
            if (documentFactory instanceof LocatorAwareDocumentFactory) {
                ((LocatorAwareDocumentFactory) documentFactory).setLocator(documentLocator);
            }

        }
    }

    static class LocatorAwareDocumentFactory extends DocumentFactory {
        private static final long serialVersionUID = 7388661832037675334L;
        private Locator locator;

        public LocatorAwareDocumentFactory() {
            super();
        }

        public void setLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public Element createElement(QName qname) {
            final LocationAwareElement element = new LocationAwareElement(qname);
            if (locator != null) {
                element.setLineNumber(locator.getLineNumber());
                element.setColumnNumber(locator.getColumnNumber());
            }
            return element;
        }

    }

    /**
     * An Element that is aware of it location (line number in) in the source document
     */
    static class LocationAwareElement extends DefaultElement {
        private static final long serialVersionUID = 260126644771458700L;
        private int lineNumber;
        private int columnNumber;

        public LocationAwareElement(QName qname) {
            super(qname);
        }

        public LocationAwareElement(QName qname, int attributeCount) {
            super(qname, attributeCount);

        }

        public LocationAwareElement(String name, Namespace namespace) {
            super(name, namespace);

        }

        public LocationAwareElement(String name) {
            super(name);

        }

        /**
         * @return the lineNumber
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * @param lineNumber the lineNumber to set
         */
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        /**
         * @return the columnNumber
         */
        public int getColumnNumber() {
            return columnNumber;
        }

        /**
         * @param columnNumber the columnNumber to set
         */
        public void setColumnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
        }

    }

    public static void applyToDom4j(SpringCloudAppConfig app, Element ele) {
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
            new DefaultMapEntry<>("subscriptionId", app.getSubscriptionId()),
            new DefaultMapEntry<>("clusterName", app.getClusterName()),
            new DefaultMapEntry<>("appName", app.getAppName()),
            new DefaultMapEntry<>("isPublic", app.getIsPublic())
        });
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                XmlUtils.addDomWithKeyValue(ele, entry.getKey(), entry.getValue());
            }
        }
    }

    public static void applyToDom4j(SpringCloudAppDeploymentConfig deployment, Element ele) {
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
            new DefaultMapEntry<>("cpu", deployment.getCpu()),
            new DefaultMapEntry<>("memoryInGB", deployment.getMemoryInGB()),
            new DefaultMapEntry<>("instanceCount", deployment.getInstanceCount()),
            new DefaultMapEntry<>("jvmOptions", deployment.getJvmOptions()),
            new DefaultMapEntry<>("runtimeVersion", deployment.getRuntimeVersion()),
        });
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                XmlUtils.addDomWithKeyValue(ele, entry.getKey(), entry.getValue());
            }
        }
    }
}
