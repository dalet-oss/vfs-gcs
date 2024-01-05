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
    private static final GcsFileNameParser INSTANCE = new GcsFileNameParser();


    /**
     * Gets singleton
     */
    public static GcsFileNameParser getInstance() {

        return INSTANCE;
    }


    private GcsFileNameParser() { }


    /**
     * Parses URI and constructs GCS file name.
     */
    @Override
    public FileName parseUri(final VfsComponentContext context, final FileName base, final String uri)
            throws FileSystemException {

        StringBuilder pathStringBuilder = new StringBuilder();

        // TODO: handle deprecation by passing an additional `Scheme[] schemes` poarameter as the first param; this
        //       array should include all valid GCS schemes, e.g. `gs` in `gs://blah`; are there any others to consider?
        String scheme = UriParser.extractScheme(uri, pathStringBuilder);

        // Normalize separators in the path
        UriParser.fixSeparators(pathStringBuilder);

        // Normalise the path
        FileType fileType = UriParser.normalisePath(pathStringBuilder);

        // Extract bucket name
        final String bucketName = UriParser.extractFirstElement(pathStringBuilder);

        return new GcsFileName(scheme, bucketName, pathStringBuilder.toString(), fileType);
    }

}
