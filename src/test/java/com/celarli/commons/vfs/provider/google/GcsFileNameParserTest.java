package com.celarli.commons.vfs.provider.google;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class GcsFileNameParserTest {

    private final GcsFileNameParser target = GcsFileNameParser.getInstance();


    @Test
    void verifyParseBasicGcsUri() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/path/to/file.txt");

        assertNotNull(fileName);

        assertEquals("gcs", fileName.getScheme());

        assertEquals("my-bucket", ((GcsFileName)fileName).getBucket());

        assertEquals("/path/to/file.txt", fileName.getPath());

        assertEquals(FileType.FILE, fileName.getType());
    }


    @Test
    void verifyDecodePercentEncodedSpacesInPath() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/path%20with%20spaces/file%20name.txt");

        assertNotNull(fileName);

        assertEquals("/path with spaces/file name.txt", fileName.getPath());
    }


    @Test
    void verifyDecodeLeadingAndTrailingSpacesInPathComponent() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/%20%20image%20import%201%20.png");

        assertNotNull(fileName);

        assertEquals("/  image import 1 .png", fileName.getPath());
    }


    @Test
    void verifyNormalizePathSeparators() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/path\\to\\file.txt");

        assertNotNull(fileName);

        assertEquals("/path/to/file.txt", fileName.getPath());
    }


    @Test
    void verifyRootBucket() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/");

        assertNotNull(fileName);

        assertEquals("gcs", fileName.getScheme());

        assertEquals("/", fileName.getPath());

        assertEquals("my-bucket", ((GcsFileName)fileName).getBucket());

        assertEquals(FileType.FOLDER, fileName.getType());
    }


    @Test
    void verifyNormalizePathSegments() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/path/../file.txt");

        assertNotNull(fileName);

        assertEquals("/file.txt", fileName.getPath());
    }


    @Test
    void verifyHandleEncodedPercentCharacter() throws Exception {

        FileName fileName = target.parseUri(null, null, "gcs://my-bucket/file%25name.txt");

        assertNotNull(fileName);

        assertEquals("/file%25name.txt", fileName.getPath());
    }
}