package org.molgenis.model;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.molgenis.model.jaxb.Entity;
import org.molgenis.model.jaxb.Field;
import org.molgenis.model.jaxb.Model;
import org.molgenis.model.jaxb.Unique;

import javax.xml.bind.JAXBException;
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

public class ModelExtractor {

    private static final Logger logger = Logger.getLogger(ModelExtractor.class.getSimpleName());


}
