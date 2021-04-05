package com.celarli.commons.vfs.provider.google;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;


public class GcsFileSystemConfigBuilder extends FileSystemConfigBuilder {

    private static final GcsFileSystemConfigBuilder BUILDER = new GcsFileSystemConfigBuilder();


    private GcsFileSystemConfigBuilder() {

        super("gcs.");
    }


    public static GcsFileSystemConfigBuilder getInstance() {

        return BUILDER;
    }


    @Override
    protected Class<? extends FileSystem> getConfigClass() {

        return GCSFileSystem.class;
    }


    /**
     * Set the input stream for key to access GCS
     */
    public void setKey(FileSystemOptions opts, byte[] fis) {

        setParam(opts, "key", fis);
    }


    /**
     * Get the input stream for key to access GCS
     */
    public byte[] getKey(FileSystemOptions opts) {

        return (byte[]) getParam(opts, "key");
    }


    /**
     * Set the hostname, will be used while constructing storage client for GCS
     */
    public void setHostname(FileSystemOptions opts, String hostname) {

        setParam(opts, "hostname", hostname);
    }


    /**
     * Get the hostname, will be used while constructing storage client for GCS
     */
    public String getHostname(FileSystemOptions opts) {

        return (String) getParam(opts, "hostname");
    }


    /**
     * Set the client type, will be used while constructing storage client for GCS
     */
    public void setClientType(FileSystemOptions opts, Integer type) {

        setParam(opts, "clientType", type);
    }


    /**
     * Get the client type, will be used while constructing storage client for GCS
     */
    public Integer getClientType(FileSystemOptions opts) {

        return (Integer) getParam(opts, "clientType");
    }


    /**
     * Set the CMK id, will be used while creating new storage item
     */
    public void setCmkId(FileSystemOptions opts, String cmkId) {

        setParam(opts, "cmkId", cmkId);
    }


    /**
     * Get the CMK id, will be used while creating new storage item
     */
    public String getCmkId(FileSystemOptions opts) {

        return (String) getParam(opts, "cmkId");
    }

}
