package org.molgenis.model;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.molgenis.MolgenisOptions;
import org.molgenis.model.jaxb.Entity;
import org.molgenis.model.jaxb.Field;
import org.molgenis.model.jaxb.Field.Type;
import org.molgenis.model.jaxb.Model;
import org.molgenis.model.jaxb.Unique;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.molgenis.model.DatabaseKeywordsStorage.COLUMN_DEF;
import static org.molgenis.model.DatabaseKeywordsStorage.COLUMN_NAME;
import static org.molgenis.model.DatabaseKeywordsStorage.COLUMN_SIZE;
import static org.molgenis.model.DatabaseKeywordsStorage.CURRENT_TIMESTAMP;
import static org.molgenis.model.DatabaseKeywordsStorage.DATA_TYPE;
import static org.molgenis.model.DatabaseKeywordsStorage.DB_DRIVER;
import static org.molgenis.model.DatabaseKeywordsStorage.DB_PASSWORD;
import static org.molgenis.model.DatabaseKeywordsStorage.DB_URI;
import static org.molgenis.model.DatabaseKeywordsStorage.DB_USER;
import static org.molgenis.model.DatabaseKeywordsStorage.FOREIGN_KEY_COLUMN_NAME;
import static org.molgenis.model.DatabaseKeywordsStorage.INDEX_NAME;
import static org.molgenis.model.DatabaseKeywordsStorage.IS_AUTOINCREMENT;
import static org.molgenis.model.DatabaseKeywordsStorage.MOLGENIS_PROPERTIES;
import static org.molgenis.model.DatabaseKeywordsStorage.NULLABLE;
import static org.molgenis.model.DatabaseKeywordsStorage.PRIMARY_KEY_COLUMN_NAME;
import static org.molgenis.model.DatabaseKeywordsStorage.REMARKS;
import static org.molgenis.model.DatabaseKeywordsStorage.TABLE_NAME;

/**
 * java.sql.Types public static final int ARRAY 2003 public static final int
 * BIGINT -5 public static final int BINARY -2 public static final int BIT -7
 * public static final int BLOB 2004 public static final int BOOLEAN 16 public
 * static final int CHAR 1 public static final int CLOB 2005 public static final
 * int DATALINK 70 public static final int DATE 91 public static final int
 * DECIMAL 3 public static final int DISTINCT 2001 public static final int
 * DOUBLE 8 public static final int FLOAT 6 public static final int INTEGER 4
 * public static final int JAVA_OBJECT 2000 public static final int LONGNVARCHAR
 * -16 public static final int LONGVARBINARY -4 public static final int
 * LONGVARCHAR -1 public static final int NCHAR -15 public static final int
 * NCLOB 2011 public static final int NULL 0 public static final int NUMERIC 2
 * public static final int NVARCHAR -9 public static final int OTHER 1111 public
 * static final int REAL 7 public static final int REF 2006 public static final
 * int ROWID -8 public static final int SMALLINT 5 public static final int
 * SQLXML 2009 public static final int STRUCT 2002 public static final int TIME
 * 92 public static final int TIMESTAMP 93 public static final int TINYINT -6
 * public static final int VARBINARY -3 public static final int VARCHAR 12
 *
 * @author Morris Swertz
 */
public class JDBCModelExtractor {
    private static final Logger logger = Logger.getLogger("JDBCModelExtractor");
    private static final String EMPTY_STRING = "";

    public static void main(String[] args) throws Exception {
        JDBCModelExtractor jdbcModelExtractor = new JDBCModelExtractor();

        Properties props = new Properties();
        props.load(new FileInputStream(MOLGENIS_PROPERTIES));

        jdbcModelExtractor.extractXml(props);

    }

    public String extractXml(MolgenisOptions options) {
        Model model = extractModel(options);
        return extractXml(model);
    }

    public String extractXml(Properties properties) {
        Model model = extractModel(properties);
        return extractXml(model);
    }

    private String extractXml(Model model) {
        try {
            return convertToString(model);
        } catch (JAXBException e) {
            logger.error("Error converting model to XML", e);
            throw new ModelExtractionException(e);
        }
    }

    public Model extractModel(MolgenisOptions options) {
        BasicDataSource dataSource = createDataSource(options);
        return extractModel(dataSource);
    }

    public Model extractModel(Properties properties) {
        BasicDataSource dataSource = createDataSource(properties);
        return extractModel(dataSource);
    }

    private BasicDataSource createDataSource(MolgenisOptions options) {
        BasicDataSource dataSource = new BasicDataSource();
        configureDataSource(dataSource, options);
        return dataSource;
    }

    private BasicDataSource createDataSource(Properties properties) {
        BasicDataSource dataSource = new BasicDataSource();
        configureDataSource(dataSource, properties);
        return dataSource;
    }

    private void configureDataSource(BasicDataSource dataSource, MolgenisOptions options) {
        dataSource.setDriverClassName(options.db_driver.trim());
        dataSource.setUsername(options.db_user.trim());
        dataSource.setPassword(options.db_password.trim());
        dataSource.setUrl(options.db_uri.trim());
    }

    private void configureDataSource(BasicDataSource dataSource, Properties properties) {
        dataSource.setDriverClassName(properties.getProperty(DB_DRIVER).trim());
        dataSource.setUsername(properties.getProperty(DB_USER).trim());
        dataSource.setPassword(properties.getProperty(DB_PASSWORD).trim());
        dataSource.setUrl(properties.getProperty(DB_URI).trim());
    }

    private Model extractModel(BasicDataSource dataSource) {
        Model model = new Model();

        try {
            Connection connection = dataSource.getConnection();

            String url = dataSource.getUrl();
            String schemaName = extractSchemaName(url);
            model.setName(schemaName);

            extractEntityTableInfo(model, connection, schemaName);

            findXrefs(model);

            findXrefLabels(model);

            findInheritanceRelationships(model);

            findMrefs(model);

            return model;
        } catch (SQLException e) {
            logger.error(e);
            throw new ModelExtractionException(e);
        }
    }

    private void extractEntityTableInfo(Model model, Connection connection, String schemaName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tableInfo = metaData.getTables(schemaName, null, null, new String[]{"TABLE"});

        while (tableInfo.next()) {
            logger.debug("TABLE: " + tableInfo);

            Entity entity = new Entity();
            entity.setName(tableInfo.getString(TABLE_NAME));
            model.getEntities().add(entity);

            ResultSet fieldInfo = metaData.getColumns(schemaName, null, tableInfo.getString(TABLE_NAME), null);
            addColumns(schemaName, metaData, tableInfo, entity, fieldInfo);

            getAutoIncrementForMySQL(connection, entity);

            ResultSet indexInfo = metaData.getIndexInfo(schemaName, null, tableInfo.getString(TABLE_NAME), true, false);
            addUniqueConstraints(entity, indexInfo);

            addAutoIdType(entity);
        }
    }

    private void addAutoIdType(Entity entity) {
        for (Field field : entity.getFields()) {
            if (field.getAuto() != null && field.getAuto() && field.getType().equals(Type.INT) && field.getUnique() != null && field.getUnique()) {
                field.setType(Type.AUTOID);
                field.setAuto(null);
                field.setUnique(null);
            }
        }
    }

    private void addUniqueConstraints(Entity entity, ResultSet indexInfo) throws SQLException {
        Map<String, List<String>> uniques = new LinkedHashMap<String, List<String>>();

        while (indexInfo.next()) {
            logger.debug("UNIQUE: " + indexInfo);

            String index = indexInfo.getString(INDEX_NAME);
            if (uniques.get(index) == null) {
                uniques.put(index, new ArrayList<String>());
            }
            uniques.get(indexInfo.getString(INDEX_NAME)).add(indexInfo.getString(INDEX_NAME));
        }

        for (List<String> indexes : uniques.values()) {
            if (indexes.size() == 1) {
                entity.getField(indexes.get(0)).setUnique(true);
            } else {
                StringBuilder fieldsBuilder = new StringBuilder();
                for (String fieldName : indexes) {
                    fieldsBuilder.append(',').append(fieldName);
                }
                Unique unique = new Unique();
                unique.setFields(fieldsBuilder.substring(1));
                entity.getUniques().add(unique);
            }
        }
    }

    private void getAutoIncrementForMySQL(Connection connection, Entity entity) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();

            String query = String.format("SELECT * FROM %s WHERE 1=0", entity.getName());
            ResultSet resultSet = statement.executeQuery(query);
            ResultSetMetaData rowMeta = resultSet.getMetaData();

            for (int i = 1; i <= rowMeta.getColumnCount(); i++) {
                if (rowMeta.isAutoIncrement(i)) {
                    entity.getFields().get(i - 1).setAuto(true);
                }
            }
        } catch (SQLException exc) {
            logger.error("Didn't retrieve autoinc/sequence: " + exc.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void addColumns(String schemaName, DatabaseMetaData metaData, ResultSet tableInfo, Entity entity, ResultSet fieldInfo) throws SQLException {
        while (fieldInfo.next()) {
            logger.debug("COLUMN: " + fieldInfo);

            Field field = new Field();
            field.setName(fieldInfo.getString(COLUMN_NAME));
            field.setType(Type.getType(fieldInfo.getInt(DATA_TYPE)));
            field.setDefaultValue(fieldInfo.getString(COLUMN_DEF));

            if (metaData.getDatabaseProductName().toLowerCase().contains("mysql")) {
                checkDateType(field);
            }

            checkForRemarks(fieldInfo, field);

            checkForNullable(fieldInfo, field);

            checkForAutoincrement(fieldInfo, field);

            checkForText(fieldInfo, field);

            ResultSet xrefInfo = metaData.getImportedKeys(schemaName, null, tableInfo.getString(TABLE_NAME));
            while (xrefInfo.next()) {
                if (xrefInfo.getString(FOREIGN_KEY_COLUMN_NAME).equals(fieldInfo.getString(COLUMN_NAME))) {
                    field.setType(Type.XREF_SINGLE);
                    field.setXrefField(xrefInfo.getString(PRIMARY_KEY_COLUMN_NAME) + "." + xrefInfo.getString(PRIMARY_KEY_COLUMN_NAME));
                }
            }

            entity.getFields().add(field);
        }
    }

    private void checkDateType(Field field) {
        if (CURRENT_TIMESTAMP.equals(field.getDefaultValue()) && (field.getType().equals(Type.DATETIME) || field.getType().equals(Type.DATE))) {
            field.setDefaultValue(null);
            field.setAuto(true);
        }
    }

    private void checkForRemarks(ResultSet fieldInfo, Field field) throws SQLException {
        if (fieldInfo.getString(REMARKS) != null && !EMPTY_STRING.equals(fieldInfo.getString(REMARKS).trim())) {
            field.setDescription(fieldInfo.getString(REMARKS));
        }
    }

    private void checkForText(ResultSet fieldInfo, Field field) throws SQLException {
        if (field.getType().equals(Type.STRING) || field.getType().equals(Type.CHAR)) {
            if (fieldInfo.getInt(COLUMN_SIZE) > 255) {
                field.setType(Type.TEXT);
                field.setLength(fieldInfo.getInt(COLUMN_SIZE));
            } else {
                if (fieldInfo.getInt(COLUMN_SIZE) != 255) {
                    field.setLength(fieldInfo.getInt(COLUMN_SIZE));
                }
                field.setType(null);
            }
        }
    }

    private void checkForAutoincrement(ResultSet fieldInfo, Field field) throws SQLException {
        if (field.getType().equals(Type.INT) && (fieldInfo.getObject(IS_AUTOINCREMENT) != null)) {
            field.setAuto(fieldInfo.getBoolean(IS_AUTOINCREMENT));
        }
    }

    private void checkForNullable(ResultSet fieldInfo, Field field) throws SQLException {
        if (fieldInfo.getBoolean(NULLABLE)) {
            field.setNillable(true);
        }
    }

    private String extractSchemaName(String url) {
        int start = url.lastIndexOf("/") + 1;
        int end = !url.contains("?") ? url.length() : url.indexOf("?");

        String schemaName = url.substring(start, end);
        logger.debug("trying to extract: " + schemaName);
        return schemaName;
    }

    private void findXrefs(Model model) {
        for (Entity sourceEntity : model.getEntities()) {
            for (Field sourceField : sourceEntity.getFields()) {
                if (Type.AUTOID.equals(sourceField.getType())) {
                    guessAndSetXrefForFields(model, sourceEntity, sourceField);
                }
            }
        }
    }

    private void guessAndSetXrefForFields(Model model, Entity entity, Field field) {
        for (Entity targetEntity : model.getEntities()) {
            for (Field targetField : targetEntity.getFields()) {
                if (isPotentialXref(field, targetField)) {
                    logger.debug("Guessed that " + targetEntity.getName() + "." + targetField.getName() + " references " + entity.getName() + "." + field.getName());
                    targetField.setType(Type.XREF_SINGLE);
                    targetField.setXrefField(entity.getName() + "." + field.getName());
                }
            }
        }
    }

    private boolean isPotentialXref(Field field, Field targetField) {
        return targetField.getName().equals(field.getName()) && targetField.getType().equals(Type.INT);
    }

    // GUESS the xref labels
    // guess the xreflabel as being the non-autoid field that is unique
    // and not null
    // rule: if there is another unique field in the referenced table
    // then that probably is usable as label
    private void findXrefLabels(Model model) {
        for (Entity entity : model.getEntities()) {
            findXrefLabelsForEntity(model, entity);
        }
    }

    private void findXrefLabelsForEntity(Model model, Entity entity) {
        for (Field field : entity.getFields()) {
            if (Type.XREF_SINGLE.equals(field.getType())) {
                String xrefEntityName = field.getXrefField().substring(0, field.getXrefField().indexOf("."));
                String xrefFieldName = field.getXrefField().substring(field.getXrefField().indexOf(".") + 1);
                // reset the xref entity to the uppercase version
                field.setXrefField(model.getEntity(xrefEntityName).getName() + "." + xrefFieldName);

                for (Field labelField : model.getEntity(xrefEntityName).getFields()) {
                    // find the other unique, nillable="false" field, if
                    // any
                    if (!labelField.getName().equals(xrefFieldName) && Boolean.TRUE.equals(labelField.getUnique()) && Boolean.FALSE.equals(labelField.getNillable())) {
                        logger.debug("guessed label " + entity.getName() + "." + labelField.getName());
                        field.setXrefLabel(labelField.getName());
                    }
                }
            }
        }
    }

    // GUESS the inheritance relationship
    // rule: if there is a foreign key that is unique itself it is
    // probably inheriting...
    // action: change to inheritance and remove the xref field
    private void findInheritanceRelationships(Model model) {
        for (Entity entity : model.getEntities()) {
            List<Field> toBeRemoved = new ArrayList<Field>();
            for (Field field : entity.getFields()) {
                if (Type.XREF_SINGLE.equals(field.getType()) && Boolean.TRUE.equals(field.getUnique())) {
                    String entityName = field.getXrefField().substring(0, field.getXrefField().indexOf("."));
                    entity.setExtends(entityName);
                    toBeRemoved.add(field);
                }
            }
            for (Field field : toBeRemoved) {
                entity.getFields().remove(field);
            }
        }
    }

    // GUESS the type="mref"
    // rule: any entity that is not a subclass and that has maximum two
    // xref fields and autoid field
    // should be a mref
    private void findMrefs(Model model) {
        List<Entity> toBeRemoved = new ArrayList<Entity>();
        for (Entity entity : model.getEntities()) {
            if (isEmptyEntity(entity)) {
                    int xrefs = 0;
                    String idField = null;
                    String localIdField = null;
                    String localEntity = null;
                    String localEntityField = null;
                    String remoteIdField = null;
                    String remoteEntity = null;
                    String remoteEntityField = null;

                    for (Field field : entity.getFields()) {
                        if (Type.AUTOID.equals(field.getType())) {
                            idField = field.getName();
                        } else if (Type.XREF_SINGLE.equals(field.getType())) {
                            xrefs++;
                            if (xrefs == 1) {
                                localIdField = field.getName();
                                localEntity = field.getXrefField().substring(0, field.getXrefField().indexOf("."));
                                localEntityField = field.getXrefField().substring(field.getXrefField().indexOf(".") + 1);
                            } else {
                                remoteIdField = field.getName();
                                remoteEntity = field.getXrefField().substring(0, field.getXrefField().indexOf("."));
                                remoteEntityField = field.getXrefField().substring(field.getXrefField().indexOf(".") + 1);
                            }
                        }
                    }

                    if (xrefs == 2 && (entity.getFields().size() == 2 || idField != null)) {
                        // add mref on 'local' end
                        Entity localContainer = model.getEntity(localEntity);
                        Field localField = new Field();
                        if (localContainer.getField(entity.getName()) == null) {
                            localField.setName(entity.getName());
                        }

                        localField.setType(Type.XREF_MULTIPLE);
                        localField.setXrefField(remoteEntity + "." + remoteEntityField);
                        localField.setMrefName(entity.getName());
                        localField.setMrefLocalid(localIdField);
                        localField.setMrefRemoteid(remoteIdField);
                        localContainer.getFields().add(localField);

                        // add mref to remote end
                        Entity remoteContainer = model.getEntity(remoteEntity);
                        Field remoteField = new Field();
                        remoteField.setType(Type.XREF_MULTIPLE);
                        remoteField.setXrefField(localEntity + "." + localEntityField);
                        remoteField.setMrefName(entity.getName());
                        // don't need to add local id as it is refering back
                        remoteField.setMrefLocalid(remoteIdField);
                        remoteField.setMrefRemoteid(localIdField);

                        if (remoteContainer.getField(entity.getName()) == null) {
                            remoteField.setName(entity.getName());
                        } else {
                            throw new ModelExtractionException("MREF creation failed: there is already a field " + remoteContainer.getName() + "." + entity.getName());
                        }

                        remoteContainer.getFields().add(remoteField);

                        // remove the link table as separate entity
                        toBeRemoved.add(entity);
                        logger.debug("guessed mref " + entity.getName());
                    }

            }
        }
        model.getEntities().removeAll(toBeRemoved);
    }

    private static boolean isEmptyEntity(Entity entity) {
        return EMPTY_STRING.equals(entity.getExtends()) && (entity.getFields().size() <= 3);
    }

    public String convertToString(Model model) throws JAXBException {
        String contextPath = "org.molgenis.model.jaxb";
        JAXBContext jaxbContext = JAXBContext.newInstance(contextPath);

        StringWriter out = new StringWriter();

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.marshal(model, out);

        return out.toString().trim();
    }
}
