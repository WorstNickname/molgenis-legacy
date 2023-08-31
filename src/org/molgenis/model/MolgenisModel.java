package org.molgenis.model;

import org.apache.log4j.Logger;
import org.molgenis.MolgenisOptions;
import org.molgenis.fieldtypes.MrefField;
import org.molgenis.fieldtypes.XrefField;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Field;
import org.molgenis.model.elements.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class MolgenisModel {
    private static final Logger logger = Logger.getLogger(MolgenisModel.class.getSimpleName());

    public static Model parse(MolgenisOptions options) throws MolgenisModelException, DatabaseException {
        try {
            logger.info("Parsing db-schema from " + options.model_database);

            Model model = MolgenisModelParser.parseDbSchema(options.model_database);

            addAuthorizableEntities(model, options.authorizable);

            logger.debug("Read: " + model);

            MolgenisModelValidator.validate(model, options);

            logger.info("parsing ui-schema");
            MolgenisModelParser.parseUiSchema(options.path + options.model_userinterface, model);

            MolgenisModelValidator.validateUI(model);

            logger.debug("validated: " + model);

            return model;
        } catch (MolgenisModelException e) {
            logger.error("Parsing failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static Model parse(Properties p) throws MolgenisModelException, DatabaseException {
        MolgenisOptions options = new MolgenisOptions(p);
        return parse(options);
    }

    public static List<Entity> sortEntitiesByDependency(List<Entity> entityList, final Model model)
            throws MolgenisModelException {
        List<Entity> result = new ArrayList<Entity>();
        List<Entity> toBeMoved = new ArrayList<Entity>();

        boolean found = true;
        while (!entityList.isEmpty() && found) {
            found = false;
            for (Entity entity : entityList) {
                List<String> deps = getDependencies(entity, model);

                boolean missing = false;
                for (String dep : deps) {
                    if (indexOf(result, dep) < 0) {
                        missing = true;
                        break;
                    }
                }

                if (!missing) {
                    toBeMoved.add(entity);
                    result.add(entity);
                    found = true;
                    break;
                }
            }

            for (Entity e : toBeMoved) {
                entityList.remove(e);
            }
            toBeMoved.clear();
        }

        // list not empty, cyclic?
        for (Entity e : entityList) {
            logger.error("cyclic relations to '" + e.getName() + "' depends on " + getDependencies(e, model));
            result.add(e);
        }

        // result
        for (Entity e : result) {
            logger.info(e.getName());
        }

        return result;
    }

    private static int indexOf(List<Entity> entityList, String entityName) {
        for (int i = 0; i < entityList.size(); i++) {
            if (entityList.get(i).getName().equals(entityName)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> getDependencies(Entity currentEntity, Model model) throws MolgenisModelException {
        Set<String> dependencies = new HashSet<String>();

        for (Field field : currentEntity.getAllFields()) {
            if (field.getType() instanceof XrefField) {
                dependencies.add(model.getEntity(field.getXrefEntityName()).getName());

                Entity xrefEntity = field.getXrefEntity();

                for (Entity e : xrefEntity.getAllDescendants()) {
                    dependencies.add(e.getName());
                }
            }
            if (field.getType() instanceof MrefField) {
                dependencies.add(field.getXrefEntity().getName());
                dependencies.addAll(model.getEntity(field.getXrefEntity().getName()).getParents());
            }
        }

        dependencies.remove(currentEntity.getName());
        return new ArrayList<String>(dependencies);
    }

    private static void addAuthorizableEntities(Model model, List<String> authorizableEntites) {
        String authorizable = "Authorizable";
        for (String entityName : authorizableEntites) {
            entityName = entityName.trim();
            Entity entity = model.getEntity(entityName);
            if (entity != null) {
                Vector<String> implNames = entity.getImplementsNames();
                if (!implNames.contains(authorizable)) {
                    implNames.add(authorizable);
                    entity.setImplements(implNames);
                }
            }
        }
    }
}
