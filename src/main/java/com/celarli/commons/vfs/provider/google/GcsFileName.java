package com.celarli.commons.vfs.provider.google;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;


public class GcsFileName extends AbstractFileName {

    private final String bucket;


    protected GcsFileName(final String scheme, final String bucket, final String path, final FileType type) {

        super(scheme, path, type);
        this.bucket = bucket;
    }


    public String getBucket() {

        return bucket;
    }


    @Override
    public FileName createName(String absPath, FileType type) {

        return new GcsFileName(getScheme(), bucket, absPath, type);
    }


    @Override
    protected void appendRootUri(StringBuilder buffer, boolean addPassword) {

        buffer.append(getScheme());
        buffer.append("://");
        buffer.append(bucket);
    }
}
