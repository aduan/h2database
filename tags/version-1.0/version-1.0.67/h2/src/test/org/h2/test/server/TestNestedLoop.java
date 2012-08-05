/*
 * Copyright 2004-2008 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.test.TestBase;

/**
 * Tests remote JDBC access with nested loops.
 * This is not allowed in some databases.
 */
public class TestNestedLoop extends TestBase {

    public void test() throws Exception {
        deleteDb("nestedLoop");
        Connection conn = getConnection("nestedLoop");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int identity, name varchar)");
        int len = getSize(1010, 10000);
        for (int i = 0; i < len; i++) {
            stat.execute("insert into test(name) values('Hello World')");
        }
        ResultSet rs = stat.executeQuery("select id from test");
        stat.executeQuery("select id from test");
        try {
            rs.next();
            error("Result set should be closed");
        } catch (SQLException e) {
            checkNotGeneralException(e);
        }
        rs = stat.executeQuery("select id from test");
        stat.close();
        try {
            rs.next();
            error("Result set should be closed");
        } catch (SQLException e) {
            checkNotGeneralException(e);
        }
        stat = conn.createStatement();
        rs = stat.executeQuery("select id from test");
        Statement stat2 = conn.createStatement();
        while (rs.next()) {
            int id = rs.getInt(1);
            ResultSet rs2 = stat2.executeQuery("select * from test where id=" + id);
            while (rs2.next()) {
                check(rs2.getInt(1), id);
                check(rs2.getString(2), "Hello World");
            }
            rs2 = stat2.executeQuery("select * from test where id=" + id);
            while (rs2.next()) {
                check(rs2.getInt(1), id);
                check(rs2.getString(2), "Hello World");
            }
        }
        conn.close();

    }

}