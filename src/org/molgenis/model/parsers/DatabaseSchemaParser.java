package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.Model;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.molgenis.model.parsers.XmlParser.parseXmlDocument;

public class DatabaseSchemaParser {

    private static final Logger logger = Logger.getLogger(DatabaseSchemaParser.class.getName());

    public static Model parseDbSchema(String xml) throws MolgenisModelException {
        Model model = new Model("molgenis");

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));

            Document document = builder.parse(is);

            parseXmlDocument(model, document);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MolgenisModelException(e.getMessage());
        }

        return model;
    }

    public static Model parseDbSchema(List<String> filenames) throws MolgenisModelException {
        Model model = new Model("molgenis");
        Document document = null;

        for (String filename : filenames) {
            DocumentBuilder builder = null;
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                document = builder.parse(filename.trim());
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                try {
                    document = builder.parse(ClassLoader.getSystemResourceAsStream(filename.trim()));
                } catch (Exception e2) {
                    logger.error("parsing of file '" + filename + "' failed.");
                    e.printStackTrace();
                    throw new MolgenisModelException("Parsing of DSL (schema) failed: " + e.getMessage());
                }
            }
            parseXmlDocument(model, document);
        }
        return model;
    }
}
