/*
 * Copyright 2004-2006 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test;

import java.sql.SQLException;
import java.util.Properties;

import org.h2.server.TcpServer;
import org.h2.test.jdbc.*;
import org.h2.test.jdbc.xa.TestXA;
import org.h2.test.db.*;
import org.h2.test.server.TestNestedLoop;
import org.h2.test.synth.TestBtreeIndex;
import org.h2.test.synth.TestCrashAPI;
import org.h2.test.synth.TestHaltApp;
import org.h2.test.synth.TestJoin;
import org.h2.test.synth.TestKill;
import org.h2.test.synth.TestMulti;
import org.h2.test.synth.TestRandomSQL;
import org.h2.test.synth.TestSynth;
import org.h2.test.unit.TestBitField;
import org.h2.test.unit.TestCache;
import org.h2.test.unit.TestCompress;
import org.h2.test.unit.TestDataPage;
import org.h2.test.unit.TestExit;
import org.h2.test.unit.TestFileLock;
import org.h2.test.unit.TestIntArray;
import org.h2.test.unit.TestIntIntHashMap;
import org.h2.test.unit.TestOverflow;
import org.h2.test.unit.TestPattern;
import org.h2.test.unit.TestReader;
import org.h2.test.unit.TestSampleApps;
import org.h2.test.unit.TestScriptReader;
import org.h2.test.unit.TestSecurity;
import org.h2.test.unit.TestStreams;
import org.h2.test.unit.TestStringCache;
import org.h2.test.unit.TestStringUtils;
import org.h2.test.unit.TestTools;
import org.h2.test.unit.TestValueHashMap;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.h2.util.StringUtils;

/**
 * @author Thomas
 */
public class TestAll {

// Snippets to run test code:
// java -cp .;%H2DRIVERS% org.h2.test.TestAll
// java -Xrunhprof:cpu=samples,depth=8 org.h2.test.TestAll
// java -Xrunhprof:heap=sites,depth=8 org.h2.test.TestAll
// C:\Programme\Java\jdk1.6.beta\bin\java

/*

Random test:

cd bin
del *.db
start cmd /k "java -cp .;%H2DRIVERS% org.h2.test.TestAll join >testJoin.txt"
start cmd /k "java org.h2.test.TestAll crash >testCrash.txt"
start cmd /k "java org.h2.test.TestAll synth >testSynth.txt"
start cmd /k "java org.h2.test.TestAll all >testAll.txt"
start cmd /k "java org.h2.test.TestAll random >testRandom.txt"
start cmd /k "java org.h2.test.TestAll btree >testBtree.txt"
start cmd /k "java org.h2.test.TestAll halt >testHalt.txt"

Test for hot spots:
java -agentlib:yjpagent=sampling,noj2ee,dir=C:\temp\Snapshots org.h2.test.bench.TestPerformance -init -db 1
java -Xmx512m -Xrunhprof:cpu=samples,depth=8 org.h2.tools.RunScript -url jdbc:h2:test;TRACE_LEVEL_FILE=3;LOG=2;MAX_LOG_SIZE=1000;DATABASE_EVENT_LISTENER='org.h2.samples.ShowProgress' -user sa -script test.sql
 */

    public boolean smallLog, big, networked, memory, ssl, textStorage, diskUndo, diskResult, deleteIndex, traceSystemOut;
    public boolean codeCoverage;
    public int logMode = 1, traceLevelFile, throttle;
    public String cipher;

    public boolean traceTest, stopOnError;
    public boolean jdk14 = true;

    private Server server;
    
    public static void main(String[] args) throws Exception {
        long time = System.currentTimeMillis();
        TestAll test = new TestAll();
        test.printSystem();
        
// pilar sms
        
/*        
CREATE TABLE T(DATA VARCHAR(10), MYINT INTEGER);
INSERT INTO T (DATA,MYINT) VALUES (null,1);
@META select ifnull(t.data, 0), myint from t;
-- 1 should be VARCHAR
select ifnull(t.data, 0), myint from t;
drop table t;
*/
        
/*        
drop all objects;
create table parent(id int primary key, parent int);
insert into parent values(1, null), (2, 1), (3, 1);
create view recursive test_view(id, parent) as 
select id, parent from parent
union all 
select parent.id, parent.parent from test_view, parent 
where parent.id = test_view.parent;
drop view test_view;
drop table parent;        
*/

//        Under certain conditions in client/server mode, obtaining text values
//        of CLOBs becomes very slow (e.g., it could take a minimum of 200 ms
//        instead of 2 ms). If I change the column type to VARCHAR, the
//        performance is very fast again.
//
//        Between 2006-12-03 and 2006-12-17, you remarked: "Very large BLOB and
//        CLOB data can now be used with the server and the cluster mode. The
//        objects will temporarily be buffered on the client side if they are
//        larger than some size (currently 64 KB)."
//
//        I think the issue is now in Transfer.java, as below:
//        272:            writer.flush();
//        273:            writeInt(LOB_MAGIC);
//
//        I believe the flush() can cause a TCP performance problem in certain
//        circumstances. The reason is that when flush() occurs, the blob
//        packets are flushed over the network, then a TCP ACK must be exchanged
//        from client to server before the separate LOB_MAGIC packet can be sent
//        from the server to client. This can cause enough latency to degrade
//        application performance noticeably.
//
//        I do not know of a good way to solve this, since you cannot avoid
//        calling writer.flush(). One "workaround" for some applications is to
//        convert CLOB to VARCHAR. However, I am currently trying something
//        different. It works so far for me. My "patch" is:
//
//        Transfer.java, at line 267:
//        java.io.OutputStream out2 = new java.io.FilterOutputStream(out) {
//        public void flush() {} };
//        Writer writer = new OutputStreamWriter(out2, Constants.UTF8);
//
//        James.        
        
        // TODO: fix Hibernate dialect bug / Bordea Felix (lost email)

        // run  TestHalt
        
        // document backup command
        
        //        WHERE FLAG does not use index, but WHERE FLAG=TRUE does
        //        drop table test;
        //        CREATE TABLE test (id int, flag BIT NOT NULL);
        //        CREATE INDEX idx_flag ON test(flag);
        //        CREATE INDEX idx_id ON test(id);
        //        insert into test values(1, false), (2, true), (3, false), (4, true);
        //        ALTER TABLE test ALTER COLUMN id SELECTIVITY 100;
        //        ALTER TABLE test ALTER COLUMN flag SELECTIVITY 1;
        //        EXPLAIN SELECT * FROM test WHERE id=2 AND flag=true; 
        //        EXPLAIN SELECT * FROM test WHERE id between 2 and 3 AND flag=true; 
        //        EXPLAIN SELECT * FROM test WHERE id=2 AND flag; 
        //
        //        ALTER TABLE test ALTER COLUMN id SELECTIVITY 1;
        //        ALTER TABLE test ALTER COLUMN flag SELECTIVITY 100;
        //        EXPLAIN SELECT * FROM test WHERE id=2 AND flag=true; 
        //        EXPLAIN SELECT * FROM test WHERE id between 2 and 3 AND flag=true; 
        //        EXPLAIN SELECT * FROM test WHERE id=2 AND flag; 

        // h2
        // update FOO set a = dateadd('second', 4320000, a);
        // ms sql server
        // update FOO set a = dateadd(s, 4320000, a);
        // mysql
        // update FOO set a = date_add(a, interval 4320000 second);
        // postgresql
        // update FOO set a = a + interval '4320000 s';
        // oracle
        // update FOO set a = a + INTERVAL '4320000' SECOND;
        
        // GroovyServlet

        // Cluster: hot deploy (adding a node on runtime)
        
        // dataSource.setLogWriter() seems to have no effect?
        
        // CHAR data type
//        DROP TABLE TEST;
//        CREATE TABLE TEST(C CHAR(10));
//        INSERT INTO TEST VALUES('1');
//        SELECT COUNT(*) FROM TEST WHERE C='1 ';
//        -- PostgreSQL, HSQLDB, MySQL, Derby, MS SQL Server, Oracle: 1
//        -- H2: 0
//        SELECT LENGTH(C), LENGTH(C || 'x') FROM TEST;
//        -- MySQL: 1, 1 (??)
//        -- MS SQL Server: 1, 11 (SELECT LEN(C), LEN(C + 'x') FROM TEST)
//        -- Oracle, Derby: 10, 11
//        -- PostgreSQL, H2, HSQLDB: 1, 2
        
        // auto-upgrade application:
        // check if new version is available 
        // (option: digital signature)
        // if yes download new version
        // (option: http, https, ftp, network)
        // backup database to SQL script
        // (option: list of databases, use recovery mechanism)
        // install new version
        
        // ftp client
        // task to download new version from another HTTP / HTTPS / FTP server
        // multi-task

        // test with PostgreSQL  Version 8.2

        // http://dev.helma.org/Wiki/RhinoLoader
        
        // test with garbage at the end of the log file (must be consistently detected as such)
        // test LIKE: compare against other databases
        // TestRandomSQL is too random; most statements fails
        // extend the random join test that compared the result against PostgreSQL
        // long running test with the same database
        // repeatable test with a very big database (making backups of the database files)
        
        // the conversion is done automatically when the new engine connects.  
        
        if(args.length>0) {
            if("crash".equals(args[0])) {
                new TestCrashAPI().runTest(test);
            } else if("synth".equals(args[0])) {
                new TestSynth().runTest(test);
            } else if("kill".equals(args[0])) {
                new TestKill().runTest(test);
            } else if("random".equals(args[0])) {
                new TestRandomSQL().runTest(test);
            } else if("join".equals(args[0])) {
                new TestJoin().runTest(test);
            } else if("btree".equals(args[0])) {
                new TestBtreeIndex().runTest(test);
            } else if("all".equals(args[0])) {
                test.testEverything();
            } else if("codeCoverage".equals(args[0])) {
                test.testCodeCoverage();
            } else if("multiThread".equals(args[0])) {
                new TestMulti().runTest(test);
            } else if("halt".equals(args[0])) {
                new TestHaltApp().runTest(test);
            }
        } else {
            test.runTests();
        }
        System.out.println("done ("+(System.currentTimeMillis()-time)+" ms)");
    }

    void runTests() throws Exception {
        
//        TODO test set lock_mode=0, 1; max_trace_file_size; modes; collation; assert
//        TODO test shutdown immediately

//        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = false;
//        logMode = 1; traceLevelFile = throttle = 0;
//        deleteIndex = textStorage = true;
//        cipher = null;

//        codeCoverage = true;

//        memory = true;
//        new TestSpeed().runTest(this);
//        new TestSpeed().runTest(this);
//        new TestSpeed().runTest(this);
//        new TestSpeed().runTest(this);



//        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = diskUndo = false;
//        traceLevelFile = throttle = 0;
//        big = true;
//        memory = false;
//
        
        testQuick();
        testCombination();

    }

    void testCodeCoverage() throws Exception {
        this.codeCoverage = true;
        runTests();
    }

    void testQuick() throws Exception {
        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = diskUndo = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        testAll();
    }

    void testEverything() throws Exception {
        for(int c = 0; c < 3; c++) {
            if(c == 0) {
                cipher = null;
            } else if(c==1) {
                cipher = "XTEA";
            } else {
                cipher = "AES";
            }
            for(int a = 0; a < 256; a++) {
                smallLog = (a & 1) != 0;
                big = (a & 2) != 0;
                networked = (a & 4) != 0;
                memory = (a & 8) != 0;
                ssl = (a & 16) != 0;
                textStorage = (a & 32) != 0;
                diskResult = (a & 64) != 0;
                deleteIndex = (a & 128) != 0;
                for(logMode = 0; logMode < 3; logMode++) {
                    traceLevelFile = logMode;
                    TestBase.printTime("cipher:" + cipher +" a:" +a+" logMode:"+logMode);
                    testAll();
                }
            }
        }
    }

    void testCombination() throws Exception {
        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        testAll();

        diskUndo = false;
        smallLog = false;
        big = false;
        networked = true;
        memory = true;
        ssl = false;
        textStorage = true;
        diskResult = deleteIndex = traceSystemOut = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        testAll();
        
        big = false;
        smallLog = false;
        networked = false;
        memory = false;
        ssl = false;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 2;
        traceLevelFile = 0;
        throttle = 0;
        cipher = null;
        testAll();        

        diskUndo = true;
        smallLog = false;
        big = networked = memory = ssl = false;
        textStorage = true;
        diskResult = true;
        deleteIndex = true;
        traceSystemOut = false;
        logMode = 1;
        traceLevelFile = 3;
        throttle = 1;
        cipher = "XTEA";
        testAll();

        diskUndo = false;
        big = true;
        smallLog = false;
        networked = false;
        memory = false;
        ssl = false;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 1;
        traceLevelFile = 1;
        throttle = 0;
        cipher = null;
        testAll();

        big = true;
        smallLog = true;
        networked = true;
        memory = false;
        ssl = true;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 2;
        traceLevelFile = 2;
        throttle = 0;
        cipher = null;
        testAll();
        
        big = true;
        smallLog = false;
        networked = true;
        memory = false;
        ssl = false;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 0;
        traceLevelFile = 0;
        throttle = 0;
        cipher = "AES";
        testAll();
        
    }

    void testAll() throws Exception {
        DeleteDbFiles.execute(TestBase.BASE_DIR, null, true);
        testDatabase();
        testUnit();
        DeleteDbFiles.execute(TestBase.BASE_DIR, null, true);
    }

    void testUnit() {
        new TestBitField().runTest(this);
        new TestCompress().runTest(this);
        new TestDataPage().runTest(this);
        new TestExit().runTest(this);
        new TestFileLock().runTest(this);
        new TestIntArray().runTest(this);
        new TestIntIntHashMap().runTest(this);
        new TestOverflow().runTest(this);
        new TestPattern().runTest(this);
        new TestReader().runTest(this);
        new TestSampleApps().runTest(this);
        new TestScriptReader().runTest(this);
        new TestSecurity().runTest(this);
        new TestStreams().runTest(this);
        new TestStringCache().runTest(this);
        new TestStringUtils().runTest(this);
        new TestTools().runTest(this);
        new TestValueHashMap().runTest(this);
    }

    void testDatabase() throws Exception {
        System.out.println("test big:"+big+" net:"+networked+" cipher:"+cipher+" memory:"+memory+" log:"+logMode+" diskResult:"+diskResult);
        beforeTest();

        // db
        new TestScriptSimple().runTest(this);
        new TestScript().runTest(this);
        new TestAutoRecompile().runTest(this);
        new TestBatchUpdates().runTest(this);
        new TestBigDb().runTest(this);
        new TestBigResult().runTest(this);
        new TestCache().runTest(this);
        new TestCases().runTest(this);
        new TestCheckpoint().runTest(this);
        new TestCluster().runTest(this);
        new TestCompatibility().runTest(this);
        new TestCsv().runTest(this);
        new TestFunctions().runTest(this);
        new TestIndex().runTest(this);
        new TestLinkedTable().runTest(this);
        new TestListener().runTest(this);
        new TestLob().runTest(this);
        new TestLogFile().runTest(this);
        new TestMemoryUsage().runTest(this);
        new TestMultiConn().runTest(this);
        new TestMultiDimension().runTest(this);
        new TestMultiThread().runTest(this);
        new TestOpenClose().runTest(this);
        new TestOptimizations().runTest(this);
        new TestPowerOff().runTest(this);
        new TestReadOnly().runTest(this);
        new TestRights().runTest(this);
        new TestRunscript().runTest(this);
        new TestSQLInjection().runTest(this);
        new TestSequence().runTest(this);
        new TestSpaceReuse().runTest(this);
        new TestSpeed().runTest(this);
        new TestTempTables().runTest(this);
        new TestTransaction().runTest(this);
        new TestTriggersConstraints().runTest(this);
        new TestTwoPhaseCommit().runTest(this);

        // server
        new TestNestedLoop().runTest(this);

        // jdbc
        new TestCancel().runTest(this);
        new TestDataSource().runTest(this);
        new TestManyJdbcObjects().runTest(this);
        new TestMetaData().runTest(this);
        new TestNativeSQL().runTest(this);
        new TestPreparedStatement().runTest(this);
        new TestResultSet().runTest(this);
        new TestStatement().runTest(this);
        new TestTransactionIsolation().runTest(this);
        new TestUpdatableResultSet().runTest(this);
        new TestXA().runTest(this);
        new TestZloty().runTest(this);

        afterTest();
    }

    public void beforeTest() throws SQLException {
        if(networked) {
            TcpServer.LOG_INTERNAL_ERRORS = true;
            String[] args = ssl ? new String[]{"-tcpSSL", "true"} : new String[0];
            server = Server.createTcpServer(args);
            try {
                server.start();
            } catch(SQLException e) {
                System.out.println("FAIL: can not start server (may already be running)");
                server = null;
            }
        }
    }

    public void afterTest() {
        if(networked && server != null) {
            server.stop();
        }
    }

    private void printSystem() {
        Properties prop = System.getProperties();
        System.out.println("Java: " +
                prop.getProperty("java.runtime.version") + ", " +
                prop.getProperty("java.vm.name")+", " +
                prop.getProperty("java.vendor"));
        System.out.println("Env: " +
                prop.getProperty("os.name") + ", " +
                prop.getProperty("os.arch")+", "+
                prop.getProperty("os.version")+", "+
                prop.getProperty("sun.os.patch.level")+", "+
                prop.getProperty("file.separator")+" "+
                prop.getProperty("path.separator")+" "+
                StringUtils.javaEncode( prop.getProperty("line.separator"))+" "+
                prop.getProperty("user.country") + " " +
                prop.getProperty("user.language") + " " +
                prop.getProperty("user.variant")+" "+
                prop.getProperty("file.encoding"));
    }
}
