/**
 * File: invengine_generate/parser/XmlParser.java <br>
 * Copyright: Inventory 2000-2006, GBIC 2005, all rights reserved <br>
 * Changelog:
 * <ul>
 * <li>2005-12-08; 1.0.0; RA Scheltema Creation.
 * </ul>
 */
package org.molgenis.model;

import org.apache.log4j.Logger;
import org.molgenis.model.elements.Model;
import org.molgenis.model.parsers.DatabaseSchemaParser;
import org.molgenis.model.parsers.UiSchemaPerser;

import java.util.List;

public class MolgenisModelParser {

	private static final Logger logger = Logger.getLogger(MolgenisModelParser.class.getName());

    public static Model parseDbSchema(String xml) throws MolgenisModelException {
        return DatabaseSchemaParser.parseDbSchema(xml);
    }

    public static Model parseDbSchema(List<String> filenames) throws MolgenisModelException {
        return DatabaseSchemaParser.parseDbSchema(filenames);
    }

    public static Model parseUiSchema(String filename, Model model) throws MolgenisModelException {
        return UiSchemaPerser.parseUiSchema(filename, model);
    }
}
