/*
 * Copyright 2004-2008 H2 Group. Licensed under the H2 License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import java.sql.SQLException;

import org.h2.message.Message;
import org.h2.message.Trace;
import org.h2.table.Table;
import org.h2.util.ObjectArray;

/**
 * Represents a role. Roles can be granted to users, and to other roles.
 */
public class Role extends RightOwner {

    private final boolean system;

    public Role(Database database, int id, String roleName, boolean system) {
        super(database, id, roleName, Trace.USER);
        this.system = system;
    }

    public String getCreateSQLForCopy(Table table, String quotedName) {
        throw Message.getInternalError();
    }

    public String getDropSQL() {
        return null;
    }

    public String getCreateSQL() {
        if (system) {
            return null;
        }
        return "CREATE ROLE " + getSQL();
    }

    public int getType() {
        return DbObject.ROLE;
    }

    public void removeChildrenAndResources(Session session) throws SQLException {
        ObjectArray users = database.getAllUsers();
        for (int i = 0; i < users.size(); i++) {
            User user = (User) users.get(i);
            Right right = user.getRightForRole(this);
            if (right != null) {
                database.removeDatabaseObject(session, right);
            }
        }
        ObjectArray roles = database.getAllRoles();
        for (int i = 0; i < roles.size(); i++) {
            Role r2 = (Role) roles.get(i);
            Right right = r2.getRightForRole(this);
            if (right != null) {
                database.removeDatabaseObject(session, right);
            }
        }
        ObjectArray rights = database.getAllRights();
        for (int i = 0; i < rights.size(); i++) {
            Right right = (Right) rights.get(i);
            if (right.getGrantee() == this) {
                database.removeDatabaseObject(session, right);
            }
        }
        database.removeMeta(session, getId());
        invalidate();
    }

    public void checkRename() throws SQLException {
    }

}