/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License, 
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.server.web;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Contains meta data information about a database schema.
 * This class is used by the H2 Console.
 */
public class DbSchema {
    
    /**
     * The database content container.
     */
    DbContents contents;
    
    /**
     * The schema name.
     */
    String name;
    
    /**
     * The quoted schema name.
     */
    String quotedName;
    
    /**
     * The table list.
     */
    DbTableOrView[] tables;
    
    /**
     * True if this is the default schema for this database.
     */
    boolean isDefault;

    DbSchema(DbContents contents, String name, boolean isDefault) {
        this.contents = contents;
        this.name = name;
        this.quotedName =  contents.quoteIdentifier(name);
        this.isDefault = isDefault;
    }

    /**
     * Read all tables for this schema from the database meta data.
     * 
     * @param meta the database meta data
     * @param tableTypes the table types to read
     */
    void readTables(DatabaseMetaData meta, String[] tableTypes) throws SQLException {
        ResultSet rs = meta.getTables(null, name, null, tableTypes);
        ArrayList list = new ArrayList();
        while (rs.next()) {
            DbTableOrView table = new DbTableOrView(this, rs);
            if (contents.isOracle && table.name.indexOf('$') > 0) {
                continue;
            }
            list.add(table);
        }
        rs.close();
        tables = new DbTableOrView[list.size()];
        list.toArray(tables);
        if (tables.length < 100) {
            for (int i = 0; i < tables.length; i++) {
                DbTableOrView tab = tables[i];
                tab.readColumns(meta);
            }
        }
    }

}