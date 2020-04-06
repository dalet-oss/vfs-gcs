package com.celarli.commons.vfs.provider.google;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;


public class GcsFileNameParser extends AbstractFileNameParser {

    /**
     * GCS file name parser instance
     */
    private static final GcsFileNameParser instance = new GcsFileNameParser();


    /**
     * Gets singleton
     */
    public static GcsFileNameParser getInstance() {

        return instance;
    }


    private GcsFileNameParser() {

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

        return new GcsFileName(scheme, bucketName, name.toString(), fileType);
    }
}