package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.fieldtypes.UnknownField;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Field;
import org.molgenis.model.elements.Index;
import org.molgenis.model.elements.Model;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import static org.molgenis.model.parsers.ElementConverter.elementValueToString;

public class EntityParser {

    private static final Logger logger = Logger.getLogger(EntityParser.class.getName());
    private static final String NAME = "name";
    private static final String LABEL = "label";
    private static final String EXTENDS = "extends";
    private static final String IMPLEMENTS = "implements";
    private static final String ABSTRACT = "abstract";
    private static final String DESCRIPTION = "description";
    private static final String SYSTEM = "system";
    private static final String DECORATOR = "decorator";
    private static final String XREF_LABEL = "xref_label";
    private static final String ALLOCATION_SIZE = "allocationSize";
    private static final String FIELD = "field";
    private static final String UNIQUE = "unique";
    private static final String FIELDS = "fields";
    private static final String KEYFIELD = "keyfield";
    private static final String INDEX = "index";
    private static final String SUBCLASS = "subclass";
    private static final String INDICES = "indices";
    private static final String INDEXFIELD = "indexfield";
    private static final String TYPE = "type";
    private static final String AUTO = "auto";
    private static final String NILLABLE = "nillable";
    private static final String OPTIONAL = "optional";
    private static final String READONLY = "readonly";
    private static final String DEFAULT = "default";
    private static final String DESC = "desc";
    private static final String HIDDEN = "hidden";
    private static final String LENGTH = "length";
    private static final String ENUM_OPTIONS = "enum_options";
    private static final String DEFAULT_CODE = "default_code";
    private static final String XREF = "xref";
    private static final String XREF_ENTITY = "xref_entity";
    private static final String XREF_FIELD = "xref_field";
    private static final String XREF_NAME = "xref_name";
    private static final String MREF_NAME = "mref_name";
    private static final String MREF_LOCALID = "mref_localid";
    private static final String MREF_REMOTEID = "mref_remoteid";
    private static final String FILTER = "filter";
    private static final String FILTER_TYPE = "filtertype";
    private static final String FILTER_FIELD = "filterfield";
    private static final String FILTER_VALUE = "filtervalue";
    private static final String XREF_CASCADE = "xref_cascade";
    private static final String JPA_CASCADE = "jpaCascade";
    private static final String[] KEYWORDS = new String[]{NAME, LABEL, EXTENDS, IMPLEMENTS, ABSTRACT, DESCRIPTION, SYSTEM, DECORATOR, XREF_LABEL, ALLOCATION_SIZE};
    private static final String[] FIELD_KEYWORDS = new String[]
            {TYPE, NAME, LABEL, AUTO, NILLABLE, OPTIONAL, READONLY, DEFAULT, DESCRIPTION, DESC,
                    UNIQUE, HIDDEN, LENGTH, INDEX, ENUM_OPTIONS, DEFAULT_CODE, XREF, XREF_ENTITY,
                    XREF_FIELD, XREF_LABEL, XREF_NAME, MREF_NAME, MREF_LOCALID, MREF_REMOTEID, FILTER,
                    FILTER_TYPE, FILTER_FIELD, FILTER_VALUE, XREF_CASCADE + "", ALLOCATION_SIZE, JPA_CASCADE};

    public static Entity parseEntity(Model model, Element element) throws MolgenisModelException {
        List<String> keyWords = new ArrayList<String>(Arrays.asList(KEYWORDS));
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            if (!keyWords.contains(element.getAttributes().item(i).getNodeName())) {
                throw new MolgenisModelException("attribute '" + element.getAttributes().item(i).getNodeName()
                        + "' not allowed for <entity>");
            }
        }

        if (element.getAttribute(NAME).trim().isEmpty()) {
            String message = "name is missing for entity " + element.toString();
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        Entity entity = new Entity(element.getAttribute(NAME).trim(), element.getAttribute(LABEL),
                model.getDatabase());
        entity.setNamespace(model.getName());

        String extendsKeyWord = element.getAttribute(EXTENDS);
        if (extendsKeyWord != null) {
            Vector<String> parents = new Vector<String>();
            StringTokenizer tokenizer = new StringTokenizer(extendsKeyWord, ",");
            while (tokenizer.hasMoreTokens()) {
                parents.add(tokenizer.nextToken().trim());
            }

            entity.setParents(parents);
        }

        String implementsKeyWord = element.getAttribute(IMPLEMENTS);
        if (implementsKeyWord != null && !implementsKeyWord.isEmpty()) {
            entity.setImplements(new Vector<String>(Arrays.asList(implementsKeyWord.split(","))));
        }

        entity.setAbstract(Boolean.parseBoolean(element.getAttribute(ABSTRACT)));

        entity.setSystem(Boolean.parseBoolean(element.getAttribute(SYSTEM)));

        String xrefLabel = element.getAttribute(XREF_LABEL);
        if (xrefLabel != null && !xrefLabel.isEmpty()) {
            List<String> xrefLabels = new ArrayList<String>();
            xrefLabels.addAll(Arrays.asList(xrefLabel.split(",")));
            entity.setXrefLabels(xrefLabels);
        } else {
            entity.setXrefLabels(null);
        }

        if (element.hasAttribute(DECORATOR)) {
            entity.setDecorator(element.getAttribute(DECORATOR));
        }

        NodeList elements = element.getElementsByTagName(DESCRIPTION);
        for (int j = 0; j < elements.getLength(); j++) {
            entity.setDescription(elementValueToString((Element) elements.item(j)));
        }

        elements = element.getElementsByTagName(FIELD);
        for (int j = 0; j < elements.getLength(); j++) {
            Element elem = (Element) elements.item(j);
            parseField(entity, elem);
        }

        elements = element.getElementsByTagName(UNIQUE);
        for (int j = 0; j < elements.getLength(); j++) {
            Element elem = (Element) elements.item(j);
            Vector<String> keys = new Vector<String>();

            if (elem.hasAttribute(FIELDS)) {
                Collections.addAll(keys, elem.getAttribute(FIELDS).split(","));
            }

            NodeList keyElements = elem.getElementsByTagName(KEYFIELD);
            for (int k = 0; k < keyElements.getLength(); k++) {
                elem = (Element) keyElements.item(k);
                String name = elem.getAttribute(NAME);
                keys.add(name);
            }

            String keyDescription = null;
            if (elem.hasAttribute(DESCRIPTION)) {
                keyDescription = elem.getAttribute(DESCRIPTION);
            }

            if (keys.isEmpty()) {
                throw new MolgenisModelException("missing fields on unique of '" + entity.getName()
                        + "'. Expected <unique fields=\"field1[,field2,..]\" description=\"...\"/>");
            }

            try {
                entity.addKey(keys, elem.getAttribute(SUBCLASS).equals("true"), keyDescription);
            } catch (Exception e) {
                throw new MolgenisModelException(e.getMessage());
            }
        }

        elements = element.getElementsByTagName(INDICES);
        if (elements.getLength() == 1) {
            Element elem = (Element) elements.item(0);

            NodeList indexElements = elem.getElementsByTagName(INDEX);
            for (int k = 0; k < indexElements.getLength(); k++) {
                elem = (Element) indexElements.item(k);

                Index index = new Index(elem.getAttribute(NAME));

                NodeList indexFieldElements = elem.getElementsByTagName(INDEXFIELD);
                for (int l = 0; l < indexFieldElements.getLength(); l++) {
                    elem = (Element) indexFieldElements.item(l);

                    Field f = entity.getField(elem.getAttribute(NAME));
                    if (f == null) {
                        throw new MolgenisModelException("Missing index field: " + elem.getAttribute(NAME));
                    }

                    try {
                        index.addField(elem.getAttribute(NAME));
                    } catch (Exception e) {
                        throw new MolgenisModelException(e.getMessage());
                    }
                }

                try {
                    entity.addIndex(index);
                } catch (Exception e) {
                }
            }
        } else if (elements.getLength() > 1) {
            throw new MolgenisModelException("Multiple indices elements");
        }

        if (element.hasAttribute(ALLOCATION_SIZE)) {
            int allocationSize = Integer.parseInt(element.getAttribute(ALLOCATION_SIZE));
            entity.setAllocationSize(allocationSize);
        }

        logger.debug("read: " + entity.getName());
        return entity;
    }

    private static void parseField(Entity entity, Element element) throws MolgenisModelException {
        List<String> keyWords = new ArrayList<String>(Arrays.asList(FIELD_KEYWORDS));
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            if (!keyWords.contains(element.getAttributes().item(i).getNodeName())) {
                throw new MolgenisModelException("attribute '" + element.getAttributes().item(i).getNodeName()
                        + "' not allowed for <field>");
            }
        }

        String type = element.getAttribute(TYPE);
        String name = element.getAttribute(NAME);
        String label = element.getAttribute(LABEL);
        String auto = element.getAttribute(AUTO);
        String nillable = element.getAttribute(NILLABLE);
        if (element.hasAttribute(OPTIONAL)) {
            nillable = element.getAttribute(OPTIONAL);
        }
        String readonly = element.getAttribute(READONLY);
        String defaultValue = element.getAttribute(DEFAULT);

        String description = element.getAttribute(DESCRIPTION);
        if (description.isEmpty()) {
            description = element.getAttribute(DESC);
        }
        String unique = element.getAttribute(UNIQUE);
        String hidden = element.getAttribute(HIDDEN);
        String length = element.getAttribute(LENGTH);
        String index = element.getAttribute(INDEX);
        String enumOptions = element.getAttribute(ENUM_OPTIONS).replace('[', ' ').replace(']', ' ').trim();
        String defaultCode = element.getAttribute(DEFAULT_CODE);
        String xrefEntity = element.getAttribute(XREF_ENTITY);
        String xrefField = element.getAttribute(XREF_FIELD);
        String xrefLabel = element.getAttribute(XREF_LABEL);
        String mrefName = element.getAttribute(MREF_NAME);
        String mrefLocalid = element.getAttribute(MREF_LOCALID);
        String mrefRemoteid = element.getAttribute(MREF_REMOTEID);

        if (type.isEmpty()) {
            type = "string";
        }

        if (element.hasAttribute(XREF) && !element.hasAttribute(XREF_FIELD)) {
            xrefField = element.getAttribute(XREF);
        }

        if (!xrefField.isEmpty() && !element.hasAttribute(XREF_ENTITY)) {
            String[] entity_field = xrefField.split("[.]");

            if (entity_field.length == 2) {
                xrefEntity = entity_field[0];
                xrefField = entity_field[1];
            }
        }

        String filter = element.getAttribute(FILTER);
        String filtertype = element.getAttribute(FILTER_TYPE);
        String filterfield = element.getAttribute(FILTER_FIELD);
        String filtervalue = element.getAttribute(FILTER_VALUE);

        if (type.equals("varchar")) {
            type = "string";
        }
        if (type.equals("number")) {
            type = "int";
        }
        if (type.equals("boolean")) {
            type = "bool";
        }
        if (type.equals("xref_single")) {
            type = "xref";
        }
        if (type.equals("xref_multiple")) {
            type = "mref";
        }
        if (label.isEmpty()) {
            label = name;
        }
        if (description.isEmpty()) {
            description = label;
        }
        if (xrefLabel == null || xrefLabel.isEmpty()) {
            xrefLabel = null;
        }
        if (type.equals("autoid")) {
            type = "int";
            nillable = "false";
            auto = "true";
            readonly = "true";
            unique = "true";
            defaultValue = "";
        }

        if (type != null && type.isEmpty()) {
            throw new MolgenisModelException("type is missing for field '" + name + "' of entity '" + entity.getName()
                    + "'");
        }
        if (MolgenisFieldTypes.getType(type) instanceof UnknownField) {
            throw new MolgenisModelException("type '" + type + "' unknown for field '" + name + "' of entity '"
                    + entity.getName() + "'");
        }
        if (name.isEmpty()) {
            throw new MolgenisModelException("name is missing for a field in entity '" + entity.getName() + "'");
        }
        if (hidden.equals("true") && !nillable.equals("true") && (defaultValue.isEmpty() && !auto.equals("true"))) {
            throw new MolgenisModelException("field '" + name + "' of entity '" + entity.getName()
                    + "' must have a default value. A field that is not nillable and hidden must have a default value.");
        }

        String jpaCascade = null;
        if (type.equals("mref") || type.equals("xref")) {
            if (element.hasAttribute("jpaCascade")) {
                jpaCascade = element.getAttribute("jpaCascade");
            }
        }

        Field field = new Field(entity, MolgenisFieldTypes.getType(type), name, label, Boolean.parseBoolean(auto),
                Boolean.parseBoolean(nillable), Boolean.parseBoolean(readonly), defaultValue, jpaCascade);
        logger.debug("read: " + field.toString());

        if (!description.isEmpty()) {
            field.setDescription(description.trim());
        }
        if (hidden.equals("true")) {
            field.setHidden(true);
        }
        if (!defaultCode.isEmpty()) {
            field.setDefaultCode(defaultCode);
        }
        if (filter.equals("true")) {
            logger.warn("filter set for field '" + name + "' of entity '" + entity.getName() + "'");
            logger.warn(filterfield + " " + filtertype + " " + filtervalue);
            logger.warn(System.currentTimeMillis() + " - filter bool: '" + Boolean.parseBoolean(filter) + "'");
            if ((filtertype != null && filtertype.isEmpty()) || (filterfield != null && filterfield.isEmpty())) {
                throw new MolgenisModelException("field '" + name + "' of entity '" + entity.getName()
                        + "': when the filter is set to true, the filtertype, filterfield and filtervalue must be set");
            }
            if (filtervalue != null && filtervalue.isEmpty()) {
                logger.warn("no value specified for filter in field '" + name + "' of entity '" + entity.getName()
                        + "'");
            }
            field.setFilter(Boolean.parseBoolean(filter));
            field.setFiltertype(filtertype);
            field.setFilterfield(filterfield);
            field.setFiltervalue(filtervalue);
        }

        if (type.equals("string")) {
            if (!length.isEmpty()) {
                field.setVarCharLength(Integer.parseInt(length));
            } else {
                field.setVarCharLength(255);
            }
        } else if (type.equals("enum")) {
            Vector<String> options = new Vector<String>();
            StringTokenizer tokenizer = new StringTokenizer(enumOptions, ",");
            while (tokenizer.hasMoreElements()) {
                options.add(tokenizer.nextToken().trim());
            }
            if (options.isEmpty()) {
                throw new MolgenisModelException("enum_options must be ',' delimited for field '" + field.getName()
                        + "' of entity '" + entity.getName() + "'");
            }

            field.setEnumOptions(options);
        } else if (type.equals("xref") || type.equals("mref")) {
            if (mrefName.isEmpty() && xrefEntity.isEmpty()) {
                throw new MolgenisModelException("xref_entity must be set for xref field '" + field.getName()
                        + "' of entity '" + entity.getName() + "'");
            }

            List<String> xref_labels = null;
            if (xrefLabel != null) {
                xref_labels = Arrays.asList(xrefLabel.split(","));
            }

            field.setXRefVariables(xrefEntity, xrefField, xref_labels);

            if (type.equals("mref")) {
                if (!mrefName.isEmpty()) {
                    field.setMrefName(mrefName);
                }
                if (!mrefLocalid.isEmpty()) {
                    field.setMrefLocalid(mrefLocalid);
                }
                if (!mrefRemoteid.isEmpty()) {
                    field.setMrefRemoteid(mrefRemoteid);
                }
            }

            if (!element.getAttribute("xref_cascade").isEmpty()) {
                if (element.getAttribute("xref_cascade").equalsIgnoreCase("true")) {
                    field.setXrefCascade(true);
                } else {
                    throw new MolgenisModelException("Unknown option on xref_cascade: '"
                            + element.getAttribute("xref_cascade") + "'");
                }
            }
        }

        try {
            entity.addField(field);

        } catch (Exception e) {
            throw new MolgenisModelException("duplicate field '" + field.getName() + "' in entity '" + entity.getName()
                    + "'");
        }

        if (index.equals("true")) {
            Index i = new Index(name);
            try {
                i.addField(name);
            } catch (Exception e) {
                throw new MolgenisModelException("duplicate field '" + field.getName() + "' in entity '"
                        + entity.getName() + "'");
            }

            entity.addIndex(i);
        }

        if (unique.equals("true")) {
            entity.addKey(field.getName(), null);
        }

        if (element.getChildNodes().getLength() >= 1) {
            String annotations = org.apache.commons.lang.StringUtils.deleteWhitespace(
                    element.getChildNodes().item(1).getTextContent()).trim();
            field.setAnnotations(annotations);
        }
    }
}
