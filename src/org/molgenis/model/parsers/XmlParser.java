package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Model;
import org.molgenis.model.elements.Module;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.molgenis.model.parsers.ElementConverter.elementValueToString;
import static org.molgenis.model.parsers.EntityParser.parseEntity;
import static org.molgenis.model.parsers.MatrixParser.parseMatrix;
import static org.molgenis.model.parsers.MethodParser.parseMethod;
import static org.molgenis.model.parsers.ViewParser.parseView;

public class XmlParser {

    private static final Logger logger = Logger.getLogger(DatabaseSchemaParser.class.getName());

    public static Document parseXmlDocument(Model model, Document document) throws MolgenisModelException {
        Element documentRoot = document.getDocumentElement();
        if (documentRoot.getAttribute("name") != null && documentRoot.getAttribute("name").isEmpty()) {
            documentRoot.setAttribute("name", "molgenis");
        }

        String modelName = documentRoot.getAttribute("name");
        String modelLabel = documentRoot.getAttribute("label");

        model.setName(modelName);
        if (!"".equals(modelLabel)) {
            model.setLabel(modelLabel);
        }

        NodeList children = documentRoot.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) child;
            if (element.getTagName().equals("module")) {
                parseModule(model, element);
            } else if (element.getTagName().equals("entity")) {
                parseEntity(model, element);
            } else if (element.getTagName().equals("matrix")) {
                parseMatrix(model, element);
            } else if (element.getTagName().equals("view")) {
                parseView(model, element);
            } else if (element.getTagName().equals("method")) {
                parseMethod(model, element);
            } else if (element.getTagName().equals("description")) {
                model.setDBDescription(model.getDBDescription() + elementValueToString(element));
            }
        }
        return document;
    }

    private static void parseModule(Model model, Element element) throws MolgenisModelException {
        // check for illegal words
        String[] keywords = new String[]
                {"name", "label"};
        List<String> key_words = new ArrayList<String>(Arrays.asList(keywords));
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            if (!key_words.contains(element.getAttributes().item(i).getNodeName())) {
                throw new MolgenisModelException("attribute '" + element.getAttributes().item(i).getNodeName()
                        + "' unknown for <module " + element.getAttribute("name") + ">");
            }
        }

        // check properties
        // NAME
        if (element.getAttribute("name").trim().isEmpty()) {
            String message = "name is missing for module " + element.toString();
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        // construct
        Module module = new Module(model.getName() + "." + element.getAttribute("name").trim(), model);

        if (element.getAttribute("label") != null && !element.getAttribute("label").isEmpty()) {
            module.setLabel(element.getAttribute("label"));
        }

        // DESCRIPTION
        NodeList elements = element.getElementsByTagName("description");
        for (int j = 0; j < elements.getLength(); j++) {
            // parse the contents, including markup...
            if (elements.item(j).getParentNode().equals(element)) {
                module.setDescription(elementValueToString((Element) elements.item(j)));
            }
        }

        // ENTITY
        elements = element.getElementsByTagName("entity");
        for (int j = 0; j < elements.getLength(); j++) {
            Element elem = (Element) elements.item(j);
            Entity e = parseEntity(model, elem);
            e.setNamespace(module.getName());
            module.getEntities().add(e);
            e.setModule(module);
        }
    }
}
