package org.molgenis.model.parsers;

import org.apache.log4j.Logger;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.Model;
import org.molgenis.model.elements.View;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ViewParser {

    private static final Logger logger = Logger.getLogger(ViewParser.class.getName());

    public static void parseView(Model model, Element element) throws MolgenisModelException {
        // get the attributes
        String name = element.getAttribute("name");
        String label = element.getAttribute("label");
        String entities = element.getAttribute("entities");

        // check properties
        if (name.isEmpty()) {
            throw new MolgenisModelException("name is missing for view " + element.toString());
        }
        if (entities.isEmpty()) {
            throw new MolgenisModelException("entities is missing for view " + element.toString());
        }
        if (label.isEmpty()) {
            label = name;
        }

        List<String> entityList = new ArrayList<String>(Arrays.asList(entities.split(",")));
        if (entityList.size() < 2) {
            throw new MolgenisModelException("a view needs at least 2 entities, define as entities=\"e1,e2\": "
                    + element.toString());
        }

        // construct the view
        View view = new View(name, label, model.getDatabase());

        // add the viewentities
        for (String viewentity : entityList) {
            if (view.getEntities().contains(viewentity)) {
                throw new MolgenisModelException("view " + name + " has duplicate viewentity entries (" + viewentity
                        + ")");
            }
            view.addEntity(viewentity);
        }
    }
}
