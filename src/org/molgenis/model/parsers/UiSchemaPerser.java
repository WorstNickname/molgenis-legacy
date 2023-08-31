package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.DBSchema;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Field;
import org.molgenis.model.elements.Form;
import org.molgenis.model.elements.Menu;
import org.molgenis.model.elements.Model;
import org.molgenis.model.elements.Plugin;
import org.molgenis.model.elements.Record;
import org.molgenis.model.elements.Tree;
import org.molgenis.model.elements.UISchema;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;

public class UiSchemaPerser {

    private static final Logger logger = Logger.getLogger(UiSchemaPerser.class.getName());

    public static Model parseUiSchema(String filename, Model model) throws MolgenisModelException {
        logger.debug("parsing ui file: " + filename);
        if (filename == null || filename.isEmpty()) {
            return model;
        }

        Document document = parseXmlFile(filename);

        // retrieve the document-root
        Element document_root = document.getDocumentElement();
        if (document_root.getAttribute("name") != null && document_root.getAttribute("name").isEmpty()
                && model.getName().isEmpty()) {
            document_root.setAttribute("name", "molgenis");
        }
        String modelName = document_root.getAttribute("name");
        model.setName(modelName);

        String label = document_root.getAttribute("label");
        if (!"".equals(label)) {
            model.setLabel(label);
        }
        // FIXME should be solved by using modules
        // alternatively ui should be in predefined dir anyway...

        // set the package name for the UI
        // model.setName("ui");

        // retrieve the children
        NodeList children = document_root.getChildNodes();

        // Menu main = new Menu("main", model.getUserinterface());
        // main.setLabel("main");
        // main.setNamespace(model.getName());
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            try {
                // root must be menu
                if (child.getNodeName().equals("description") || child.getNodeName().equals("form")
                        || child.getNodeName().equals("plugin") || child.getNodeName().equals("menu")
                        || child.getNodeName().equals("include")) {
                    parseUiSchema(model, (Element) child, model.getUserinterface());
                } else {
                    throw new MolgenisModelException("Unrecognized element: " + child.getNodeName());
                }

                // }
            } catch (Exception e) {
                e.printStackTrace();
                throw new MolgenisModelException(e.getMessage());
            }

        }
        return model;
    }

    private static void parseUiSchema(Model model, Element element, UISchema parent) throws MolgenisModelException {
        UISchema new_parent = null;

        if (element.getTagName().equals("include")) {
            String fileName = element.getAttribute("file");
            if (fileName == null || fileName.isEmpty()) {
                throw new MolgenisModelException("include failed: no file attribute set");
            }
            try {
                Document document_root = parseXmlFile(fileName);
                // include all its elements, ignore the root node
                NodeList rootChildren = document_root.getChildNodes();
                for (int i = 0; i < rootChildren.getLength(); i++) {
                    NodeList children = rootChildren.item(i).getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if (child.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        try {

                            parseUiSchema(model, (Element) child, parent);

                        } catch (Exception e) {
                            throw new MolgenisModelException(e.getMessage());
                        }

                    }
                }

            } catch (Exception e) {
                throw new MolgenisModelException("include failed: " + e.getMessage());
            }
        } else if (element.getTagName().equals("description")) {
            logger.warn("currently the '<description>' tag is ignored in ui.xml");
        } else {

            String name = element.getAttribute("name").trim();
            String namespace = model.getName();
            String label = element.getAttribute("label");
            String group = element.getAttribute("group");
            String groupRead = element.getAttribute("groupRead");

            // check required properties
            if ((name == null || name.isEmpty()) && !element.getTagName().equals("form")) {
                throw new MolgenisModelException("name is missing for subform of screen '" + parent.getName() + "'");
            }
            if (label != null && label.isEmpty()) {
                label = name;
            }

            if (group.isEmpty()) {
                group = null; // TODO: Discuss with Erik/Morris/Robert!
            }

            if (groupRead.isEmpty()) {
                groupRead = null; // TODO: Discuss with Erik/Morris/Robert!
            }

            // add this element to the meta-model
            if (element.getTagName().equals("menu")) {
                Menu menu = new Menu(name, parent);
                menu.setLabel(label);
                menu.setGroup(group);
                menu.setGroupRead(groupRead);
                menu.setNamespace(namespace);

                if (group != null && groupRead != null && group.equals(groupRead)) {
                    throw new MolgenisModelException(
                            "You cannot assign both read/write and read rights on a single menu");
                }

                if (element.getAttribute("position") == null || !element.getAttribute("position").isEmpty()) {
                    menu.setPosition(Menu.Position.getPosition(element.getAttribute("position")));
                }

                new_parent = menu;
            } else if (element.getTagName().equals("form")) {
                if (name.isEmpty()) {
                    name = element.getAttribute("entity");
                }
                Form form = new Form(name, parent);
                form.setLabel(label);
                form.setGroup(group);
                form.setGroupRead(groupRead);

                if (group != null && groupRead != null && group.equals(groupRead)) {
                    throw new MolgenisModelException(
                            "You cannot assign both read/write and read rights on a single form");
                }

                /** Optional custom header for the selected form screen */
                String header = element.getAttribute("header");
                if (!header.isEmpty()) form.setHeader(header);

                /** Optional description for the selected form screen */
                String description = element.getAttribute("description");
                if (!description.isEmpty()) form.setDescription(description);

                form.setNamespace(namespace);
                new_parent = form;

                // VIEWTYPE
                if (element.getAttribute("view").equals("record")) {
                    element.setAttribute("view", "edit");
                }
                if (element.getAttribute("view").isEmpty()) {
                    if (element.getChildNodes().getLength() > 0) {
                        element.setAttribute("view", "edit");
                    } else {
                        element.setAttribute("view", "list");
                    }
                }
                if (Form.ViewType.parseViewType(element.getAttribute("view")) == Form.ViewType.VIEWTYPE_UNKNOWN) {
                    throw new MolgenisModelException("view '" + element.getAttribute("view") + "' unknown for form '"
                            + form.getName() + "'");
                }
                form.setViewType(Form.ViewType.parseViewType(element.getAttribute("view")));

                // LIMIT
                form.setLimit(10);
                String limit = element.getAttribute("limit");
                if (limit != null && !limit.isEmpty()) {
                    form.setLimit(Integer.parseInt(limit));
                }

                // ACTIONS
                form.setCommands(new ArrayList<String>());
                String commands = element.getAttribute("commands");
                if (commands != null && !commands.isEmpty()) {
                    String[] commandArray = commands.split(",");
                    for (String command : commandArray) {
                        form.getCommands().add(command.trim());
                    }
                }

                // SORT
                String sortby = element.getAttribute("sortby");
                if (sortby != null && !sortby.isEmpty()) {
                    // TODO ensure valid sort field
                    form.setSortby(sortby);
                }
                String sortorder = element.getAttribute("sortorder");
                if (sortorder != null && !sortorder.isEmpty()) {
                    if (!sortorder.equalsIgnoreCase(Form.SortOrder.ASC.toString())
                            && !sortorder.equalsIgnoreCase(Form.SortOrder.DESC.toString())) {
                        throw new MolgenisModelException(
                                "sortorder can only be 'asc' or 'desc'. Parser found <form name=\"" + form.getName()
                                        + "\" sortorder=\"" + sortorder + "\"");
                    } else {

                        form.setSortorder(Form.SortOrder.parse(sortorder));
                    }
                }

                // FILTER
                String filter = element.getAttribute("filter");
                if (filter != null && filter.equals("true")) {
                    if (element.getAttribute("filterfield") != null && element.getAttribute("filterfield").isEmpty()) {
                        throw new MolgenisModelException("filterfield is missing for subform of screen '"
                                + parent.getName() + "'");
                    }
                    if (element.getAttribute("filtertype") != null && element.getAttribute("filtertype").isEmpty()) {
                        throw new MolgenisModelException("filtertype is missing for subform of screen '"
                                + parent.getName() + "'");
                    }
                    if (element.getAttribute("filtervalue") != null && element.getAttribute("filtervalue").isEmpty()) {
                        logger.warn("filtervalue is missing for subform of screen '" + parent.getName() + "'");
                    }
                    form.setFilter(true);
                    form.setFilterfield(element.getAttribute("filterfield"));
                    form.setFiltertype(element.getAttribute("filtertype"));
                    form.setFiltervalue(element.getAttribute("filtervalue"));
                }

                // READONLY
                form.setReadOnly(false);
                String readonly = element.getAttribute("readonly");
                if (readonly != null) {
                    form.setReadOnly(Boolean.parseBoolean(readonly));
                }

                // ENTITY
                // TODO: whould have expected this in the constructor!
                Entity entity = (Entity) model.getDatabase().getChild(element.getAttribute("entity"));
                if (entity == null) {
                    throw new MolgenisModelException("Could not find the specified entity '"
                            + element.getAttribute("entity") + "' for form '" + form.getName() + "'");
                }
                form.setRecord((Record) entity);// form.setEntity(entity);

                // HIDDEN FIELDS
                form.setHideFields(new ArrayList<String>());
                String hide_fields = element.getAttribute("hide_fields");
                if (hide_fields != null && !hide_fields.isEmpty()) {
                    String[] hiddenFieldArray = hide_fields.split(",");
                    for (String field : hiddenFieldArray) {
                        Field f = entity.getAllField(field.trim());
                        if (f == null) {
                            throw new MolgenisModelException("Could not find field '" + field
                                    + "' defined in hide_fields='" + element.getAttribute("hide_fields")
                                    + "' in form '" + form.getName() + "'");
                        }
                        // use name from 'f' to correct for case problems
                        form.getHideFields().add(f.getName());
                    }
                }

                // COMPACT_FIELDS
                if (!element.getAttribute("compact_view").isEmpty()) {
                    String[] fields = element.getAttribute("compact_view").split(",");
                    // check if the fields are there
                    List<String> compact_fields = new ArrayList<String>();
                    for (String field : fields) {
                        Field f = entity.getAllField(field);
                        if (f == null) {
                            throw new MolgenisModelException("Could not find field '" + field
                                    + "' defined in compact_view='" + element.getAttribute("compact_view")
                                    + "' in form '" + form.getName() + "'");
                        }
                        // use name from 'f' to correct for case problems

                        compact_fields.add(form.getEntity().getName() + "_" + f.getName());
                    }
                    form.setCompactView(compact_fields);
                }
            } else if (element.getTagName().equals("tree")) {
                // check required properties
                if (element.getAttribute("parentfield") != null && element.getAttribute("parentfield").isEmpty()) {
                    throw new MolgenisModelException("parentfield is missing for tree screen '" + name + "'");
                }
                if (element.getAttribute("idfield") != null && element.getAttribute("idfield").isEmpty()) {
                    throw new MolgenisModelException("idfield is missing for tree screen '" + name + "'");
                }
                if (element.getAttribute("labelfield") != null && element.getAttribute("labelfield").isEmpty()) {
                    throw new MolgenisModelException("labelfield is missing for tree screen '" + name + "'");
                }

                Tree tree = new Tree(name, parent, element.getAttribute("parentfield"),
                        element.getAttribute("idfield"), element.getAttribute("labelfield"));
                tree.setLabel(label);
                tree.setGroup(group);
                tree.setGroupRead(groupRead);

                if (group != null && groupRead != null && group.equals(groupRead)) {
                    throw new MolgenisModelException(
                            "You cannot assign both read/write and read rights on a single tree");
                }

                tree.setNamespace(namespace);
                new_parent = tree;

                // READONLY
                tree.setReadOnly(true);
                String readonly = element.getAttribute("readonly");
                if (readonly != null) {
                    tree.setReadOnly(Boolean.parseBoolean(readonly));
                }

                // ENTITY
                // TODO: whould have expected this in the constructor!
                DBSchema entity = model.getDatabase().getChild(element.getAttribute("entity"));
                if (entity == null) {
                    throw new MolgenisModelException("Could not find the specified entity '"
                            + element.getAttribute("entity") + "'");
                }
                tree.setRecord((Record) entity);
            } else if (element.getTagName().equals("plugin")) {
                if (element.getAttribute("type") != null && element.getAttribute("type").isEmpty()) {
                    throw new MolgenisModelException("plugin has no name");
                }
                Plugin plugin = new Plugin(name, parent, element.getAttribute("type"));
                plugin.setLabel(label);
                plugin.setGroup(group);
                plugin.setGroupRead(groupRead);

                if (group != null && groupRead != null && group.equals(groupRead)) {
                    throw new MolgenisModelException(
                            "You cannot assign both read/write and read rights on a single plugin");
                }

                plugin.setNamespace(namespace);
                new_parent = plugin;

                // METHOD
                String method = element.getAttribute("flavor");
                if (!"".equals(method)) {
                    plugin.setPluginMethod(Plugin.Flavor.getPluginMethod(method));
                }

                // READONLY
                plugin.setReadOnly(false);
                String readonly = element.getAttribute("readonly");
                if (readonly != null) {
                    plugin.setReadOnly(Boolean.parseBoolean(readonly));
                }
            }
            /*
             * else { // this is the unexpected throw new Exception("Encountered
             * unknown element: " + element.getTagName()); }
             */

            // recurse the children
            NodeList children = element.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                parseUiSchema(model, (Element) child, new_parent);
            }
        }
    }

    private static Document parseXmlFile(String filename) throws MolgenisModelException {
        Document document = null;
        DocumentBuilder builder = null;
        try {
            // initialize the document
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(filename);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            try {
                // try to load from classpath
                document = builder.parse(ClassLoader.getSystemResourceAsStream(filename.trim()));
            } catch (Exception e2) {
                logger.error("parsing of file '" + filename + "' failed.");
                e.printStackTrace();
                throw new MolgenisModelException("Parsing of DSL (ui) failed: " + e.getMessage());
            }
        }
        return document;
    }
}
