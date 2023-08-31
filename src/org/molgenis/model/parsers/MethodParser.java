package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Method;
import org.molgenis.model.elements.MethodQuery;
import org.molgenis.model.elements.Model;
import org.molgenis.model.elements.Parameter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MethodParser {

    private static final Logger logger = Logger.getLogger(MethodParser.class.getName());
    private static final String FIELD = "field";
    private static final String OPERATOR = "operator";
    private static final String PARAMETER = "parameter";
    private static final String RETURN = "return";
    private static final String QUERY = "query";
    private static final String DESCRIPTION = "description";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String ENTITY = "entity";
    public static final String LABEL = "label";

    public static void parseMethod(Model model, Element element) throws MolgenisModelException {
        if (element.getAttribute(NAME).isEmpty()) {
            String message = "Name is missing for method " + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        Method method = new Method(element.getAttribute(NAME), model.getMethodSchema());

        NodeList nodes = element.getChildNodes();
        for (int nodeId = 0; nodeId < nodes.getLength(); ++nodeId) {
            Node node = nodes.item(nodeId);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (((Element) node).getTagName().equals(DESCRIPTION)) {
                method.setDescription(((Element) node).getTextContent().trim());
            } else if (((Element) node).getTagName().equals(PARAMETER)) {
                parseParameter(method, (Element) node);
            } else if (((Element) node).getTagName().equals(RETURN)) {
                parseReturnType(model, method, (Element) node);
            } else if (((Element) node).getTagName().equals(QUERY)) {
                parseQuery(model, method, (Element) node);
            }
        }
    }

    private static void parseParameter(Method method, Element element) throws MolgenisModelException {
        if (element.getAttribute(NAME).isEmpty()) {
            String message = "Name is missing for parameter " + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }
        if (element.getAttribute(TYPE).isEmpty()) {
            String message = "Type is missing for parameter " + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        Parameter parameter = new Parameter(method, Parameter.Type.getType(element.getAttribute(TYPE)),
                element.getAttribute(NAME), element.getAttribute(LABEL), false, element.getAttribute("default"));

        try {
            method.addParameter(parameter);
        } catch (Exception e) {
            throw new MolgenisModelException("duplicate parameter '" + parameter.getName() + "' in method '"
                    + method.getName() + "'");
        }
    }

    private static void parseReturnType(Model model, Method method, Element element) throws MolgenisModelException {
        if (element.getAttribute(TYPE).isEmpty()) {
            String message = "Type is missing for returntype " + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        Entity entity = model.getEntity(element.getAttribute(TYPE));

        try {
            method.setReturnType(entity);
        } catch (Exception e) {
            throw new MolgenisModelException(e.getMessage());
        }
    }

    private static void parseQuery(Model model, Method method, Element element) throws MolgenisModelException {
        if (element.getAttribute(ENTITY).isEmpty()) {
            String message = "Type is missing for returntype " + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        MethodQuery query = new MethodQuery(element.getAttribute(ENTITY));
        method.setQuery(query);

        NodeList nodes = element.getChildNodes();
        for (int nodeid = 0; nodeid < nodes.getLength(); ++nodeid) {
            Node node = nodes.item(nodeid);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (((Element) node).getTagName().equals("rule")) {
                parseQueryRule(query, (Element) node);
            }
        }
    }

    private static void parseQueryRule(MethodQuery query, Element element) throws MolgenisModelException {
        String msgFormat = "Type is missing for %s";
        if (element.getAttribute(FIELD).isEmpty()) {
            String message = String.format(msgFormat, FIELD) + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }
        if (element.getAttribute(OPERATOR).isEmpty()) {
            String message = String.format(msgFormat, OPERATOR) + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }
        if (element.getAttribute(PARAMETER).isEmpty()) {
            String message = String.format(msgFormat, PARAMETER) + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        query.addRule(new MethodQuery.Rule(
                element.getAttribute(FIELD),
                element.getAttribute(OPERATOR),
                element.getAttribute(PARAMETER))
        );
    }
}
