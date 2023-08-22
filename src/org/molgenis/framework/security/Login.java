package org.molgenis.framework.security;

import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.ui.ScreenController;
import org.molgenis.util.Entity;

import java.util.List;

public interface Login {
    boolean login(Database db, String name, String password) throws Exception;

    void logout(Database db) throws Exception;

    void reload(Database db) throws Exception;

    boolean isAuthenticated();

    String getUserName();

    Integer getUserId();

    boolean isLoginRequired();

    boolean canRead(Class<? extends Entity> entityClass) throws DatabaseException;


    boolean canRead(Entity entity) throws DatabaseException;

    boolean canRead(ScreenController<?> screen) throws DatabaseException;

    boolean canWrite(Class<? extends Entity> entityClass) throws DatabaseException;

    boolean canWrite(Entity entity) throws DatabaseException;

    QueryRule getRowlevelSecurityFilters(Class<? extends Entity> klazz);

    String getRedirect();

    void setAdmin(List<? extends Entity> entities, Database db) throws DatabaseException;

    void setRedirect(String redirect);
}
