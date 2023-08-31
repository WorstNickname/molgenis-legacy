package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.Matrix;
import org.molgenis.model.elements.Model;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatrixParser {

    private static final Logger logger = Logger.getLogger(MatrixParser.class.getName());
    private static final String NAME = "name";
    private static final String COLUMN = "col";
    private static final String ROW = "row";
    private static final String CONTENT_ENTITY = "content_entity";
    private static final String CONTAINER = "container";
    private static final String COLUMN_ENTITY = "col_entity";
    private static final String ROW_ENTITY = "row_entity";
    private static final String CONTENT = "content";
    private static final String[] keywords = new String[]
            {NAME, COLUMN, CONTENT, CONTAINER, ROW, COLUMN, ROW_ENTITY, COLUMN_ENTITY};

    public static void parseMatrix(Model model, Element element) throws MolgenisModelException {
        List<String> keyWords = new ArrayList<String>(Arrays.asList(keywords));
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            if (!keyWords.contains(element.getAttributes().item(i).getNodeName())) {
                throw new MolgenisModelException("attribute '" + element.getAttributes().item(i).getNodeName()
                        + "' unknown for <entity>");
            }
        }

        if (element.getAttribute("name").isEmpty()) {
            String message = "name is missing for entity " + element;
            logger.error(message);
            throw new MolgenisModelException(message);
        }

        Matrix matrix = new Matrix(element.getAttribute(NAME), model.getDatabase());
        matrix.setCol(element.getAttribute(COLUMN));
        matrix.setRow(element.getAttribute(ROW));
        matrix.setContentEntity(element.getAttribute(CONTENT_ENTITY));
        matrix.setContainer(element.getAttribute(CONTAINER));
        matrix.setColEntityName(element.getAttribute(COLUMN_ENTITY));
        matrix.setRowEntityName(element.getAttribute(ROW_ENTITY));
        matrix.setContent(element.getAttribute(CONTENT));

        logger.debug("read: " + matrix);
    }
}
