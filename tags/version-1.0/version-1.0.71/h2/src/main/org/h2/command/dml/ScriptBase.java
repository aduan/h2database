/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License, 
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import org.h2.command.Prepared;
import org.h2.constant.ErrorCode;
import org.h2.constant.SysProperties;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.message.Message;
import org.h2.security.SHA256;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.FileStoreInputStream;
import org.h2.store.FileStoreOutputStream;
import org.h2.tools.CompressTool;
import org.h2.util.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.SmallLRUCache;
import org.h2.value.Value;

/**
 * This class is the base for RunScriptCommand and ScriptCommand.
 */
public abstract class ScriptBase extends Prepared implements DataHandler {

    /**
     * The output stream.
     */
    protected OutputStream out;

    /**
     * The input stream.
     */
    protected InputStream in;

    /**
     * The file name (if set).
     */
    private Expression file;
    private String fileName;

    private String cipher;
    private byte[] key;
    private FileStore store;
    private String compressionAlgorithm;

    public ScriptBase(Session session) {
        super(session);
    }

    public void setCipher(String c) {
        cipher = c;
    }

    protected boolean isEncrypted() {
        return cipher != null;
    }

    public void setPassword(char[] password) {
        SHA256 sha = new SHA256();
        key = sha.getKeyPasswordHash("script", password);
    }

    public void setFile(Expression file) {
        this.file = file;
    }
    
    protected String getFileName() throws SQLException {
        if (file != null && fileName == null) {
            fileName = file.getValue(session).getString();
            if (fileName == null || fileName.trim().length() == 0) {
                fileName = "script.sql";
            }
            fileName = SysProperties.scriptDirectory + fileName;
        }
        return fileName;
    }

    public boolean isTransactional() {
        return false;
    }

    protected void deleteStore() throws SQLException {
        String fileName = getFileName();
        if (fileName != null) {
            FileUtils.delete(fileName);
        }
    }

    private void initStore() throws SQLException {
        byte[] magic = Database.getMagic(true);
        Database db = session.getDatabase();
        // script files are always in text format
        String fileName = getFileName();        
        store = FileStore.open(db, fileName, "rw", magic, cipher, key);
        store.setCheckedWriting(false);
        store.init();
    }

    protected void openOutput() throws SQLException {
        String fileName = getFileName();        
        if (fileName == null) {
            return;
        }
        if (isEncrypted()) {
            initStore();
            out = new FileStoreOutputStream(store, this, compressionAlgorithm);
            // always use a big buffer, otherwise end-of-block is written a lot
            out = new BufferedOutputStream(out, Constants.IO_BUFFER_SIZE_COMPRESS);
        } else {
            OutputStream o = FileUtils.openFileOutputStream(fileName, false);
            out = new BufferedOutputStream(o, Constants.IO_BUFFER_SIZE);
            out = CompressTool.wrapOutputStream(out, compressionAlgorithm, Constants.SCRIPT_SQL);
        }
    }

    protected void openInput() throws SQLException {
        String fileName = getFileName();        
        if (fileName == null) {
            return;
        }
        if (isEncrypted()) {
            initStore();
            in = new FileStoreInputStream(store, this, compressionAlgorithm != null, false);
        } else {
            InputStream inStream;
            try {
                inStream = FileUtils.openFileInputStream(fileName);
            } catch (IOException e) {
                throw Message.convertIOException(e, fileName);
            }
            in = new BufferedInputStream(inStream, Constants.IO_BUFFER_SIZE);
            in = CompressTool.wrapInputStream(in, compressionAlgorithm, Constants.SCRIPT_SQL);
            if (in == null) {
                throw Message.getSQLException(ErrorCode.FILE_NOT_FOUND_1, Constants.SCRIPT_SQL + " in " + fileName);
            }
        }
    }

    protected void closeIO() {
        IOUtils.closeSilently(out);
        out = null;
        IOUtils.closeSilently(in);
        in = null;
        if (store != null) {
            store.closeSilently();
            store = null;
        }
    }

    public boolean needRecompile() {
        return false;
    }

    public boolean getTextStorage() {
        // script files are always in text format
        return true;
    }

    public String getDatabasePath() {
        return null;
    }

    public FileStore openFile(String name, String mode, boolean mustExist) throws SQLException {
        return null;
    }

    public int getChecksum(byte[] data, int start, int end) {
        return session.getDatabase().getChecksum(data, start, end);
    }

    public void checkPowerOff() throws SQLException {
        session.getDatabase().checkPowerOff();
    }

    public void checkWritingAllowed() throws SQLException {
        session.getDatabase().checkWritingAllowed();
    }

    public void freeUpDiskSpace() throws SQLException {
        session.getDatabase().checkWritingAllowed();
    }

    public void handleInvalidChecksum() throws SQLException {
        session.getDatabase().handleInvalidChecksum();
    }

    public int compareTypeSave(Value a, Value b) throws SQLException {
        throw Message.getInternalError();
    }

    public int getMaxLengthInplaceLob() {
        return session.getDatabase().getMaxLengthInplaceLob();
    }

    public int allocateObjectId(boolean b, boolean c) {
        return session.getDatabase().allocateObjectId(b, c);
    }

    public String createTempFile() throws SQLException {
        return session.getDatabase().createTempFile();
    }

    public String getLobCompressionAlgorithm(int type) {
        return session.getDatabase().getLobCompressionAlgorithm(type);
    }

    public void setCompressionAlgorithm(String algorithm) {
        this.compressionAlgorithm = algorithm;
    }

    public Object getLobSyncObject() {
        return this;
    }

    public boolean getLobFilesInDirectories() {
        return session.getDatabase().getLobFilesInDirectories();
    }

    public SmallLRUCache getLobFileListCache() {
        return null;
    }

}