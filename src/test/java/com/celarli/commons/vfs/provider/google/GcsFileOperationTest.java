package com.celarli.commons.vfs.provider.google;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;


public class GcsFileOperationTest {

    //@Test
    public void testCopy() throws Exception {

        String fileName = "" + UUID.randomUUID();

        // Now let's create a temp file just for upload
        File temp = File.createTempFile(fileName, ".tmp");

        try (FileWriter fw = new FileWriter(temp)) {
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append("testing...");
            bw.flush();
        }

        DefaultFileSystemManager fileSystemManager = new DefaultFileSystemManager();
        fileSystemManager.addProvider("gcs", new GcsFileProvider());
        fileSystemManager.init();

        // Create a URL for creating this remote file
        String bucket = "npd-test";
        String currUriStr = String.format("%s://%s/%s", "gcs", bucket, fileName);

        FileSystemOptions fileSystemOptions = new FileSystemOptions();
        GcsFileSystemConfigBuilder.getInstance().setClientType(fileSystemOptions, 1);

        // Resolve the imaginary file remotely. So we have a file object
        FileObject gcsFile = fileSystemManager.resolveFile(currUriStr, fileSystemOptions);

        // Resolve the local file for upload
        FileObject localFile = fileSystemManager
                .resolveFile(String.format("file://%s", temp.getAbsolutePath()), fileSystemOptions);

        // Use the API to copy from one local file to the remote file
        gcsFile.copyFrom(localFile, Selectors.SELECT_SELF);

        // Delete the temp we don't need anymore
        temp.delete();
        localFile.delete();
    }

}
