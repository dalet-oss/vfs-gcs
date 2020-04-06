package com.celarli.commons.vfs.provider.google;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;


public class GCSFileNameParser extends AbstractFileNameParser {

    /**
     * GCS file name parser instance
     */
    private static final GCSFileNameParser instance = new GCSFileNameParser();


    /**
     * Gets singleton
     */
    public static GCSFileNameParser getInstance() {

        return instance;
    }


    private GCSFileNameParser() {

    }


    /**
     * Parses URI and constructs GCS file name.
     */
    @Override
    public FileName parseUri(final VfsComponentContext context, final FileName base, final String uri)
            throws FileSystemException {

        StringBuilder name = new StringBuilder();

        String scheme = UriParser.extractScheme(uri, name);

        UriParser.normalisePath(name);

        // Normalize separators in the path
        UriParser.fixSeparators(name);

        // Normalise the path
        FileType fileType = UriParser.normalisePath(name);

        // Extract bucket name
        final String bucketName = UriParser.extractFirstElement(name);

        return new GCSFileName(scheme, bucketName, name.toString(), fileType);
    }
}