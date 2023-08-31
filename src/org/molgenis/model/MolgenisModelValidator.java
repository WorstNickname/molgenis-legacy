package org.molgenis.model;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.MolgenisOptions;
import org.molgenis.fieldtypes.EnumField;
import org.molgenis.fieldtypes.IntField;
import org.molgenis.fieldtypes.MrefField;
import org.molgenis.fieldtypes.StringField;
import org.molgenis.fieldtypes.TextField;
import org.molgenis.fieldtypes.XrefField;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Field;
import org.molgenis.model.elements.Form;
import org.molgenis.model.elements.Model;
import org.molgenis.model.elements.Module;
import org.molgenis.model.elements.UISchema;
import org.molgenis.model.elements.Unique;
import org.molgenis.model.elements.View;
import org.molgenis.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import static org.molgenis.model.DatabaseKeywordsStorage.HSQL_KEYWORDS;
import static org.molgenis.model.DatabaseKeywordsStorage.JAVASCRIPT_KEYWORDS;
import static org.molgenis.model.DatabaseKeywordsStorage.JAVA_KEYWORDS;
import static org.molgenis.model.DatabaseKeywordsStorage.MYSQL_KEYWORDS;
import static org.molgenis.model.DatabaseKeywordsStorage.ORACLE_KEYWORDS;
import static org.molgenis.model.DatabaseKeywordsStorage.ORACLE;
import static org.molgenis.model.DatabaseKeywordsStorage.MYSQL;
import static org.molgenis.model.DatabaseKeywordsStorage.HSQL;

public class MolgenisModelValidator {
    private static final Logger logger = Logger.getLogger(MolgenisModelValidator.class.getSimpleName());

    public static void validate(Model model, MolgenisOptions options) throws MolgenisModelException, DatabaseException {
        logger.debug("Validating model and adding defaults:");

        validateNamesAndReservedWords(model, options);
        validateExtendsAndImplements(model);

        if (options.object_relational_mapping.equals(MolgenisOptions.SUBCLASS_PER_TABLE)) {
            addTypeFieldInSubclasses(model);
        }

        validateKeys(model);
        addXrefLabelsToEntities(model);
        validatePrimaryKeys(model);
        validateForeignKeys(model);
        validateViews(model);

        correctXrefCaseSensitivity(model);
        moveMrefsFromInterfaceAndCopyToSubclass(model);
        createLinkTablesForMrefs(model, options);

        copyDefaultXrefLabels(model);
        copyDecoratorsToSubclass(model);

        if (options.object_relational_mapping.equals(MolgenisOptions.CLASS_PER_TABLE)) {
            addInterfaces(model);
        }

        copyFieldsToSubclassToEnforceConstraints(model);

        validateNameSize(model, options);
    }

    /**
     * As mrefs are a linking table between to other tables, interfaces cannot
     * be part of mrefs (as they don't have a linking table). To solve this
     * issue, mrefs will be removed from interface class and copied to subclass.
     *
     * @throws MolgenisModelException
     */
    public static void moveMrefsFromInterfaceAndCopyToSubclass(Model model) throws MolgenisModelException {
        logger.debug("copy fields to subclass for constrain checking...");

        for (Entity entity : model.getEntities()) {
            copyMrefsFromInterfaces(entity);
        }

        for (Entity entity : model.getEntities()) {
            if (entity.isAbstract()) {
                for (Field mref : entity.getFieldsOf(new MrefField())) {
                    entity.removeField(mref);
                }
            }
        }
    }

    private static void copyMrefsFromInterfaces(Entity entity) throws MolgenisModelException {
        for (Entity iface : entity.getImplements()) {
            for (Field mref : iface.getFieldsOf(new MrefField())) {
                Field field = new Field(mref);
                field.setEntity(entity);

                String mrefName = entity.getName() + "_" + field.getName();
                if (mrefName.length() > 30) {
                    mrefName = mrefName.substring(0, 25) + Integer.toString(mrefName.hashCode()).substring(0, 5);
                }
                field.setMrefName(mrefName);
                entity.addField(0, field);
            }
        }
    }

    public static void validateNameSize(Model model, MolgenisOptions options) throws MolgenisModelException {
        for (Entity entity : model.getEntities()) {
            if (options.db_driver.toLowerCase().contains(ORACLE) && entity.getName().length() > 30) {
                throw new MolgenisModelException(String.format("table name %s is longer than %d", entity.getName(), 30));
            }
            for (Field field : entity.getFields()) {
                if (options.db_driver.toLowerCase().contains(ORACLE) && field.getName().length() > 30) {
                    throw new MolgenisModelException(String.format("field name %s is longer than %d", field.getName(), 30));
                }
            }
        }
    }

    public static void validateUI(Model model) throws MolgenisModelException {
        logger.debug("validating UI and adding defaults:");

        validateHideFields(model);
    }

    public static void validateHideFields(Model model) throws MolgenisModelException {
        for (Form form : model.getUserinterface().getAllForms()) {
            List<String> hideFields = form.getHideFields();
            for (String fieldName : hideFields) {
                Entity entity = form.getEntity();
                Field field = entity.getAllField(fieldName);
                if (field == null) {
                    throw new MolgenisModelException("error in hide_fields for form name=" + form.getName()
                            + ": cannot find field '" + fieldName + "' in form entity='" + entity.getName() + "'");
                } else {
                    if (!form.getReadOnly() && !field.isNillable() && !field.isAuto() && field.getDefaultValue().equals("")) {
                        logger.warn("you can get trouble with hiding field '" + fieldName + "' for form name="
                                + form.getName()
                                + ": record is not null and doesn't have a default value (unless decorator fixes this!");
                    }
                }
            }
        }
    }

    public static void addXrefLabelsToEntities(Model model) throws MolgenisModelException {
        for (Entity entity : model.getEntities()) {
            if (entity.getXrefLabels() == null) {
                List<String> result = new ArrayList<String>();
                if (entity.getAllKeys().size() > 1) {
                    for (Field field : entity.getAllKeys().get(1).getFields()) {
                        result.add(field.getName());
                    }
                    entity.setXrefLabels(result);
                } else if (!entity.getAllKeys().isEmpty()) {
                    for (Field field : entity.getAllKeys().get(0).getFields()) {
                        result.add(field.getName());
                    }
                    entity.setXrefLabels(result);
                }
                logger.debug("added default xref_label=" + entity.getXrefLabels() + " to entity=" + entity.getName());
            }
        }
    }

    public static void validatePrimaryKeys(Model model) throws MolgenisModelException {
        for (Entity entity : model.getEntities())
            if (!entity.isAbstract() && (entity.getKeys().isEmpty())) {
                throw new MolgenisModelException("entity '" + entity.getName() + " doesn't have a primary key defined ");
            }
    }

    /**
     * Default xref labels can come from: - the xref_entity (or one of its
     * superclasses)
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void copyDefaultXrefLabels(Model model) throws MolgenisModelException {
        for (Entity entity : model.getEntities()) {
            for (Field field : entity.getFields()) {
                if (field.getType() instanceof XrefField
                        || field.getType() instanceof MrefField
                        && (!field.getXrefLabelNames().isEmpty()
                        && field.getXrefLabelNames().get(0).equals(field.getXrefFieldName()))) {
                    Entity xrefEntity = field.getXrefEntity();
                    if (xrefEntity.getXrefLabels() != null) {
                        logger.debug("copying xref_label " + xrefEntity.getXrefLabels() + " from "
                                + field.getXrefEntityName() + " to field " + field.getEntity().getName() + "."
                                + field.getName());
                        field.setXrefLabelNames(xrefEntity.getXrefLabels());
                    }
                }
            }
        }
    }

    /**
     * In each entity of an entity subclass hierarchy a 'type' field is added to
     * enable filtering. This method adds this type as 'enum' field such that
     * all subclasses are an enum option.
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void addTypeFieldInSubclasses(Model model) throws MolgenisModelException {
        logger.debug("add a 'type' field in subclasses to enable instanceof at database level...");
        for (Entity entity : model.getEntities()) {
            if (entity.isRootAncestor()) {
                Vector<Entity> subclasses = entity.getAllDescendants();
                Vector<String> enumOptions = new Vector<String>();
                enumOptions.add(firstToUpper(entity.getName()));
                for (Entity subclass : subclasses) {
                    enumOptions.add(firstToUpper(subclass.getName()));
                }
                if (entity.getField(Field.TYPE_FIELD) == null) {
                    Field typeField = new Field(entity, new EnumField(), Field.TYPE_FIELD, Field.TYPE_FIELD, true, false,
                            true, null);
                    typeField.setDescription("Subtypes have to be set to allow searching");
                    typeField.setHidden(true);
                    entity.addField(0, typeField);
                }
                entity.getField(Field.TYPE_FIELD).setEnumOptions(enumOptions);
            } else {
                entity.removeField(entity.getField(Field.TYPE_FIELD));
            }
        }

    }

    /**
     * Add link tables for many to many relationships
     * <ul>
     * <li>A link table entity will have the name of [from_entity]_[to_entity]
     * <li>A link table has two xrefs to the from/to entity respectively
     * <li>The column names are those of the respective fields
     * <li>In case of a self reference, the second column name is '_self'
     * </ul>
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void createLinkTablesForMrefs(Model model, MolgenisOptions options) throws MolgenisModelException {
        logger.debug("add linktable entities for mrefs...");

        for (Entity xrefEntityFrom : model.getEntities()) {
            for (Field xrefFieldFrom : xrefEntityFrom.getImplementedFieldsOf(new MrefField())) {
                Entity xrefEntityTo = xrefFieldFrom.getXrefEntity();
                Field xrefFieldTo = xrefFieldFrom.getXrefField();

                String mrefName = xrefFieldFrom.getMrefName();

                if (options.db_driver.toLowerCase().contains(ORACLE) && mrefName.length() > 30) {
                    throw new MolgenisModelException("mref name cannot be longer then 30 characters, found: "
                            + mrefName);
                }

                Entity mrefEntity = model.getEntity(mrefName);

                if (mrefEntity == null) {
                    mrefEntity = new Entity(mrefName, mrefName, model.getDatabase());
                    mrefEntity.setNamespace(xrefEntityFrom.getNamespace());
                    mrefEntity.setAssociation(true);
                    mrefEntity.setDescription("Link table for many-to-many relationship '"
                            + xrefEntityFrom.getName() + "." + xrefFieldFrom.getName() + "'.");
                    mrefEntity.setSystem(true);

                    Field idField = new Field(mrefEntity, new IntField(), "autoid", "autoid", true, false, false,
                            null);
                    idField.setHidden(true);
                    idField.setDescription("automatic id field to ensure ordering of mrefs");
                    mrefEntity.addField(idField);
                    mrefEntity.addKey(idField.getName(), "unique auto key to ensure ordering of mrefs");

                    Vector<String> unique = new Vector<String>();

                    Field field = new Field(mrefEntity, new XrefField(), xrefFieldFrom.getMrefRemoteid(), null, false,
                            false, false, null);
                    field.setXRefVariables(xrefEntityTo.getName(), xrefFieldTo.getName(),
                            xrefFieldFrom.getXrefLabelNames());
                    if (xrefFieldFrom.isXrefCascade()) {
                        field.setXrefCascade(true);
                    }
                    mrefEntity.addField(field);

                    unique.add(field.getName());

                    for (Field key : xrefEntityFrom.getKeyFields(Entity.PRIMARY_KEY)) {
                        field = new Field(mrefEntity, new XrefField(), xrefFieldFrom.getMrefLocalid(), null,
                                false, false, false, null);

                        field.setXRefVariables(xrefEntityFrom.getName(), key.getName(), null);

                        mrefEntity.addField(field);
                        unique.add(field.getName());
                    }
                    mrefEntity.addKey(unique, false, null);
                } else {
                    Field xrefField = mrefEntity.getAllField(xrefFieldTo.getName());
                    if (xrefField != null) {
                        xrefField.setXrefLabelNames(xrefFieldFrom.getXrefLabelNames());
                    }
                }
                xrefFieldFrom.setMrefName(mrefEntity.getName());
            }
        }

    }

    /**
     * Check if the view objects are an aggregate of known entities.
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void validateViews(Model model) throws MolgenisModelException {
        for (View view : model.getViews()) {
            Vector<Entity> entities = new Vector<Entity>();
            Vector<Pair<Entity, Entity>> references = new Vector<Pair<Entity, Entity>>();

            for (String viewEntity : view.getEntities()) {
                Entity entity = model.getEntity(viewEntity);
                if (entity == null) {
                    throw new MolgenisModelException("Entity '" + viewEntity + "' in view '"
                            + view.getName() + "' does not exist");
                }
                entities.add(entity);
            }

            for (Entity entity : entities) {
                for (Field field : entity.getFields()) {
                    if (!(field.getType() instanceof XrefField)) {
                        continue;
                    }
                    Entity referenced = field.getXrefEntity();

                    for (Entity other : entities) {
                        if (other.getName().equals(entity.getName())) {
                            continue;
                        }
                        if (other.getName().equals(referenced.getName())) {
                            references.add(new Pair<Entity, Entity>(entity, other));
                        }
                    }
                }
            }
            Vector<Entity> viewEntities = new Vector<Entity>();
            for (Pair<Entity, Entity> pair : references) {
                if (!viewEntities.contains(pair.getA())) {
                    viewEntities.add(pair.getA());
                }
                if (!viewEntities.contains(pair.getB())) {
                    viewEntities.add(pair.getB());
                }
            }
        }
    }

    /**
     * Validate foreign key relationships: <li>
     * <ul>
     * Do the xref_field and xref_label refer to fields actually exist
     * <ul>
     * Is the entity refered to non-abstract
     * <ul>
     * Does the xref_field refer to a unique field (i.e. foreign key)</li>
     *
     * @param model
     * @throws MolgenisModelException
     * @throws DatabaseException
     */
    public static void validateForeignKeys(Model model) throws MolgenisModelException, DatabaseException {
        logger.debug("validate xref_field and xref_label references...");

        // validate foreign key relations
        for (Entity entity : model.getEntities()) {
            String entityName = entity.getName();

            for (Field field : entity.getFields()) {
                String fieldName = field.getName();
                if (field.getType() instanceof XrefField || field.getType() instanceof MrefField) {

                    String xrefEntityName = field.getXrefEntityName();
                    String xrefFieldName = field.getXrefFieldName();

                    List<String> xrefLabelNames = field.getXrefLabelNames();

                    if (xrefLabelNames.isEmpty()) {
                        xrefLabelNames.add(field.getXrefFieldName());
                    }

                    Entity xrefEntity = model.getEntity(xrefEntityName);
                    if (xrefEntity == null) {
                        throw new MolgenisModelException("xref entity '" + xrefEntityName
                                + "' does not exist for field " + entityName + "." + fieldName);
                    }

                    if (xrefFieldName == null || xrefFieldName.equals("")) {
                        xrefFieldName = xrefEntity.getPrimaryKey().getName();
                        field.setXrefField(xrefFieldName);

                        logger.debug("automatically set " + entityName + "." + fieldName + " xref_field="
                                + xrefFieldName);
                    }

                    if (!xrefEntity.getName().equals(field.getXrefEntityName())) {
                        throw new MolgenisModelException(
                                "xref entity '" + xrefEntityName + "' does not exist for field " + entityName + "."
                                        + fieldName + " (note: entity names are case-sensitive)");
                    }

                    if (xrefEntity.isAbstract()) {
                        throw new MolgenisModelException("cannot refer to abstract xref entity '" + xrefEntityName
                                + "' from field " + entityName + "." + fieldName);
                    }

                    Field xrefField = xrefEntity.getField(xrefFieldName, false, true, true);

                    if (xrefField == null) {
                        throw new MolgenisModelException("xref field '" + xrefFieldName
                                + "' does not exist for field " + entityName + "." + fieldName);
                    }

                    for (String xrefLabelName : xrefLabelNames) {
                        Field xrefLabel;
                        if (xrefLabelName.contains(".")) {
                            xrefLabel = model.findField(xrefLabelName);
                        } else {
                            xrefLabel = xrefEntity.getAllField(xrefLabelName);
                        }
                        if (xrefLabel == null) {
                            StringBuilder validFieldsBuilder = new StringBuilder();
                            Map<String, List<Field>> candidates = field.allPossibleXrefLabels();

                            if (candidates.isEmpty()) {
                                throw new MolgenisModelException(
                                        "xref label '"
                                                + xrefLabelName
                                                + "' does not exist for field "
                                                + entityName
                                                + "."
                                                + fieldName
                                                + ". \nCouldn't find suitable secondary keys to use as xref_label. \nDid you set a unique=\"true\" or <unique fields=\" ...>?");
                            }

                            for (Entry<String, List<Field>> entry : candidates.entrySet()) {
                                String key = entry.getKey();
                                if (xrefLabelName.equals(key)) {
                                    List<Field> value = entry.getValue();
                                    xrefLabel = value.get(value.size() - 1);
                                }
                                validFieldsBuilder.append(',').append(key);
                            }

                            if (xrefLabel == null) {
                                throw new MolgenisModelException("xref label '" + xrefLabelName
                                        + "' does not exist for field " + entityName + "." + fieldName
                                        + ". Valid labels include " + validFieldsBuilder.toString());
                            }
                        } else {
                            if (!xrefLabelName.equals(xrefFieldName)
                                    && !field.allPossibleXrefLabels().containsKey(xrefLabelName)) {
                                String validLabels = StringUtils.join(field.allPossibleXrefLabels().keySet(), ',');
                                throw new MolgenisModelException("xref label '" + xrefLabelName + "' for "
                                        + entityName + "." + fieldName
                                        + " is not part a secondary key. Valid labels are " + validLabels
                                        + "\nDid you set a unique=\"true\" or <unique fields=\" ...>?");
                            }
                        }
                    }

                    if (xrefField.getType() instanceof TextField) {
                        throw new MolgenisModelException("xref field '"
                                + xrefFieldName + "' is of illegal type 'TEXT' for field " + entityName + "." + fieldName);
                    }

                    boolean isUnique = false;
                    for (Unique unique : xrefEntity.getAllKeys()) {
                        for (Field keyfield : unique.getFields()) {
                            if (keyfield.getName().equals(xrefFieldName)) {
                                isUnique = true;
                                break;
                            }
                        }
                    }
                    if (!isUnique) {
                        throw new MolgenisModelException("xref pointer '" + xrefEntityName + "."
                                + xrefFieldName + "' is a non-unique field for field " + entityName + "." + fieldName
                                + "\n" + xrefEntity.toString());
                    }
                }
            }
        }
    }

    /**
     * Validate the unique constraints
     * <ul>
     * <li>Do unique field names refer to existing fields?
     * <li>Is there a unique column id + unique label?
     * </ul>
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void validateKeys(Model model) throws MolgenisModelException {
        logger.debug("validate the fields used in 'unique' constraints...");
        for (Entity entity : model.getEntities()) {
            String entityname = entity.getName();
            int autocount = 0;
            for (Field field : entity.getAllFields()) {
                String fieldName = field.getName();
                if (field.isAuto() && field.getType() instanceof IntField) {
                    autocount++;

                    boolean iskey = false;

                    for (Unique unique : entity.getAllKeys()) {
                        for (Field keyfield : unique.getFields()) {
                            if (keyfield.getName() == null) {
                                throw new MolgenisModelException("unique field '"
                                        + fieldName + "' is not known in entity " + entityname);
                            }
                            if (keyfield.getName().equals(field.getName())) {
                                iskey = true;
                            }
                        }
                    }

                    if (!iskey) {
                        throw new MolgenisModelException(
                                "there can be only one auto column and it must be the primary key for field '" + entityname
                                        + "." + fieldName + "'");
                    }
                }

                if (field.getType() instanceof EnumField
                        && (field.getDefaultValue() != null
                        && !"".equals(field.getDefaultValue())
                        && (!field.getEnumOptions().contains(field.getDefaultValue())))) {
                    throw new MolgenisModelException("default value '" + field.getDefaultValue()
                            + "' is not in enum_options for field '" + entityname + "." + fieldName + "'");
                }
            }

            if (autocount > 1) {
                throw new MolgenisModelException(
                        "there should be only one auto column and it must be the primary key for entity '" + entityname
                                + "'");
            }

            if (!entity.isAbstract() && autocount < 1) {
                throw new MolgenisModelException(
                        "there should be one auto column for each root entity and it must be the primary key for entity '"
                                + entityname + "'");
            }
        }
    }

    /**
     * Validate extends and implements relationships:
     * <ul>
     * <li>Do superclasses actually exist
     * <li>Do 'implements' refer to abstract superclasses (interfaces)
     * <li>Do 'extends' refer to non-abstract superclasses
     * <li>Copy primary key to subclass to form parent/child relationships
     * </ul>
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void validateExtendsAndImplements(Model model) throws MolgenisModelException {
        logger.debug("validate 'extends' and 'implements' relationships...");
        for (Entity entity : model.getEntities()) {
            List<Entity> ifaces = entity.getAllImplements();
            for (Entity iface : ifaces) {
                if (!iface.isAbstract()) {
                    throw new MolgenisModelException(entity.getName() + " cannot implement "
                            + iface.getName() + " because it is not abstract");
                }
                try {
                    Field pkeyField = null;
                    if (iface.getKeyFields(Entity.PRIMARY_KEY).size() == 1) {
                        pkeyField = iface.getKeyFields(Entity.PRIMARY_KEY).get(0);
                        if (entity.getField(pkeyField.getName()) == null) {
                            Field field = new Field(pkeyField);
                            field.setEntity(entity);
                            field.setAuto(pkeyField.isAuto());
                            field.setNillable(pkeyField.isNillable());
                            field.setReadonly(pkeyField.isReadOnly());
                            field.setXRefVariables(iface.getName(), pkeyField.getName(), null);
                            field.setHidden(true);

                            logger.debug("copy primary key " + field.getName() + " from interface " + iface.getName()
                                    + " to " + entity.getName());
                            entity.addField(field);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MolgenisModelException(e.getMessage());
                }
            }

            Vector<String> parents = entity.getParents();
            if (!parents.isEmpty()) {
                Entity parent = model.getEntity(parents.get(0));
                if (parent == null) {
                    throw new MolgenisModelException("superclass '" + parents.get(0) + "' for '"
                            + entity.getName() + "' is missing");
                }
                if (parent.isAbstract()) {
                    throw new MolgenisModelException(entity.getName() + " cannot extend "
                            + parents.get(0) + " because superclas " + parents.get(0) + " is abstract (use implements)");
                }
                if (entity.isAbstract()) {
                    throw new MolgenisModelException(entity.getName() + " cannot extend "
                            + parents.get(0) + " because " + entity.getName() + " itself is abstract");
                }
            }
        }
    }

    /**
     * Add interfaces as artificial entities to the model
     *
     * @param model
     * @throws MolgenisModelException
     * @throws Exception
     */
    public static void addInterfaces(Model model) throws MolgenisModelException {
        logger.debug("add root entities for interfaces...");
        for (Entity entity : model.getEntities()) {
            if (entity.isRootAncestor()) {
                Entity rootAncestor = entity;
                if (!entity.isAbstract()) {

                    rootAncestor = new Entity("_" + entity.getName() + "Interface", entity.getName(),
                            model.getDatabase());
                    rootAncestor.setDescription("Identity map table for "
                            + entity.getName()
                            + " and all its subclasses. "
                            + "For each row that is added to "
                            + entity.getName()
                            + " or one of its subclasses, first a row must be added to this table to get a valid primary key value.");

                    Vector<Field> keyFields = entity.getKey(0).getFields();
                    Vector<String> keyFieldsCopy = new Vector<String>();
                    for (Field field : keyFields) {
                        Field keyField = new Field(rootAncestor, field.getType(), field.getName(), field.getName(), field.isAuto(),
                                field.isNillable(), field.isReadOnly(), field.getDefaultValue());
                        keyField.setDescription("Primary key field unique in " + entity.getName()
                                + " and its subclasses.");
                        if (keyField.getType() instanceof StringField) {
                            keyField.setVarCharLength(keyField.getVarCharLength());
                        }
                        rootAncestor.addField(keyField);
                        keyFieldsCopy.add(keyField.getName());
                    }
                    rootAncestor.addKey(keyFieldsCopy, entity.getKey(0).isSubclass(), null);

                    Vector<String> parents = new Vector<String>();
                    parents.add(rootAncestor.getName());
                    entity.setParents(parents);
                }

                Vector<Entity> subclasses = entity.getAllDescendants();
                Vector<String> enumOptions = new Vector<String>();
                enumOptions.add(entity.getName());
                for (Entity subclass : subclasses) {
                    enumOptions.add(subclass.getName());
                }
                Field typeField = new Field(rootAncestor, new EnumField(), Field.TYPE_FIELD, Field.TYPE_FIELD, true,
                        false, false, null);
                typeField.setDescription("Subtypes of " + entity.getName() + ". Have to be set to allow searching");
                typeField.setEnumOptions(enumOptions);
                typeField.setHidden(true);
                rootAncestor.addField(0, typeField);
            }
        }
    }

    public static void validateNamesAndReservedWords(Model model, MolgenisOptions options)
            throws MolgenisModelException {
        logger.debug("check for JAVA and SQL reserved words...");
        List<String> keywords = new ArrayList<String>();
        keywords.addAll(Arrays.asList(JAVA_KEYWORDS));
        keywords.addAll(Arrays.asList(JAVASCRIPT_KEYWORDS));
        keywords.addAll(Arrays.asList(ORACLE_KEYWORDS));

        if (options.db_driver.contains(MYSQL)) {
            keywords.addAll(Arrays.asList(MYSQL_KEYWORDS));
        }
        if (options.db_driver.contains(HSQL)) {
            keywords.addAll(Arrays.asList(HSQL_KEYWORDS));
        }

        if (model.getName().contains(" ")) {
            throw new MolgenisModelException("model name '" + model.getName()
                    + "' illegal: it cannot contain spaces. Use 'label' if you want to show a name with spaces.");
        }
        for (Module module : model.getModules()) {
            if (module.getName().contains(" ")) {
                throw new MolgenisModelException("module name '" + module.getName()
                        + "' illegal: it cannot contain spaces. Use 'label' if you want to show a name with spaces.");
            }
        }

        for (Entity entity : model.getEntities()) {
            if (entity.getName().contains(" ")) {
                throw new MolgenisModelException("entity name '" + entity.getName()
                        + "' cannot contain spaces. Use 'label' if you want to show a name with spaces.");
            }

            if (keywords.contains(entity.getName().toUpperCase()) || keywords.contains(entity.getName().toLowerCase())) {
                throw new MolgenisModelException("entity name '" + entity.getName() + "' illegal:" + entity.getName()
                        + " is a reserved JAVA and/or SQL word and cannot be used for entity name");
            }
            for (Field field : entity.getFields()) {
                if (field.getName().contains(" ")) {
                    throw new MolgenisModelException("field name '" + entity.getName() + "." + field.getName()
                            + "' cannot contain spaces. Use 'label' if you want to show a name with spaces.");
                }

                if (keywords.contains(field.getName().toUpperCase()) || keywords.contains(field.getName().toLowerCase())) {
                    throw new MolgenisModelException("field name '" + entity.getName() + "." + field.getName() + "' illegal: "
                            + field.getName() + " is a reserved JAVA and/or SQL word");
                }

                if (field.getType() instanceof XrefField || field.getType() instanceof MrefField) {
                    String xrefEntity = field.getXrefEntityName();
                    if (xrefEntity != null
                            && (keywords.contains(xrefEntity.toUpperCase()) || keywords.contains(xrefEntity
                            .toLowerCase()))) {
                        throw new MolgenisModelException("xref_entity reference from field '" + entity.getName() + "."
                                + field.getName() + "' illegal: " + xrefEntity + " is a reserved JAVA and/or SQL word");
                    }

                    if (field.getType() instanceof MrefField) {
                        if (field.getMrefName() == null) {
                            String mrefEntityName = field.getEntity().getName() + "_" + field.getName();

                            if (mrefEntityName.length() > 30) {
                                mrefEntityName = mrefEntityName.substring(0, 25)
                                        + Integer.toString(mrefEntityName.hashCode()).substring(0, 5);
                            }

                            Entity mrefEntity = null;
                            try {
                                mrefEntity = model.getEntity(mrefEntityName);
                            } catch (Exception exc) {
                                throw new MolgenisModelException("mref name for " + field.getEntity().getName() + "."
                                        + field.getName() + " not unique. Please use explicit mref_name=name setting");
                            }

                            if (mrefEntity != null) {
                                mrefEntityName += "_mref";
                                if (model.getEntity(mrefEntityName) != null) {
                                    mrefEntityName += "_" + Math.random();
                                }
                            }

                            field.setMrefName(mrefEntityName);
                        }
                        if (field.getMrefLocalid() == null) {
                            field.setMrefLocalid(field.getEntity().getName());
                        }
                        if (field.getMrefRemoteid() == null) {
                            field.setMrefRemoteid(field.getName());
                        }
                    }
                }
            }
        }

        for (UISchema screen : model.getUserinterface().getAllChildren()) {
            if (screen.getName().contains(" ")) {
                throw new MolgenisModelException(
                        "ui element '"
                                + screen.getName()
                                + "illegal: it cannot contain spaces. If you want to have a name with spaces use the 'label' attribute");
            }
        }
    }

    /**
     * test for case sensitivity
     */
    public static void correctXrefCaseSensitivity(Model model) throws MolgenisModelException {
        logger.debug("correct case of names in xrefs...");
        for (Entity entity : model.getEntities()) {
            for (Field field : entity.getFields()) {

                if (field.getType() instanceof XrefField || field.getType() instanceof MrefField) {
                    Entity xrefEntity = field.getXrefEntity();
                    field.setXRefEntity(xrefEntity.getName());

                    String xrefField = field.getXrefField().getName();

                    List<String> xrefLabels = field.getXrefLabelsTemp();
                    List<String> correctedXrefLabels = new ArrayList<String>();
                    for (String xrefLabel : xrefLabels) {
                        correctedXrefLabels.add(xrefEntity.getAllField(xrefLabel).getName());
                    }
                    field.setXRefVariables(xrefEntity.getName(), xrefField, correctedXrefLabels);
                }
            }
        }
    }

    /**
     * @param model
     * @throws MolgenisModelException
     */
    public static void copyDecoratorsToSubclass(Model model) throws MolgenisModelException {
        logger.debug("copying decorators to subclasses...");
        for (Entity entity : model.getEntities()) {
            if (entity.getDecorator() == null) {
                for (Entity superClass : entity.getImplements()) {
                    if (superClass.getDecorator() != null) {
                        entity.setDecorator(superClass.getDecorator());
                    }
                }
                for (Entity superClass : entity.getAllAncestors()) {
                    if (superClass.getDecorator() != null) {
                        entity.setDecorator(superClass.getDecorator());
                    }
                }
            }
        }
    }

    /**
     * Copy fields to subclasses (redundantly) so this field can be part of an
     * extra constraint. E.g. a superclass has non-unique field 'name'; in the
     * subclass it is said to be unique and a copy is made to capture this
     * constraint in the table for the subclass.
     *
     * @param model
     * @throws MolgenisModelException
     */
    public static void copyFieldsToSubclassToEnforceConstraints(Model model) throws MolgenisModelException {
        logger.debug("copy fields to subclass for constrain checking...");
        for (Entity entity : model.getEntities()) {
            if (entity.hasAncestor()) {
                for (Unique key : entity.getKeys()) {
                    for (Field field : key.getFields()) {
                        if (entity.getField(field.getName()) == null) {
                            Field copy = new Field(field);
                            copy.setEntity(entity);
                            copy.setAuto(field.isAuto());
                            entity.addField(copy);

                            logger.debug(key + " cannot be enforced on " + entity.getName() + ", copying "
                                    + field.getEntity().getName() + "." + field.getName() + " to subclass as " + copy.getName());
                        }
                    }
                }
            }
        }
    }

    private static String firstToUpper(String string) {
        if (string == null) {
            return " NULL ";
        }
        if (string.length() > 0) {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        } else {
            return " ERROR[STRING EMPTY] ";
        }
    }
}