# vfs-gcs
Google Cloud Storage provider for Apache Commons VFS - http://commons.apache.org/proper/commons-vfs/


## Origins

The code in this repo was originally derived from https://github.com/ltouati/vfs-gcs.  That repo is only intermittently
maintained, and no releases have been issued since 2019.  As a result, we continue to develop this repo independently,
and occasionally offer our changes upstream.


## Builds, releases etc.

This project is built using Travis CI.
[![Build Status](https://travis-ci.com/dalet-oss/vfs-gcs.svg?branch=master)](https://travis-ci.com/dalet-oss/vfs-gcs)

Published artifacts are available on Maven Central as `com.github.dalet-oss:vfs-gcs`.

For the latest version, see https://github.com/dalet-oss/vfs-gcs/releases.

#### Note for maintainers:

-  Every push to master gets built, but not published
-  To publish artifacts, it is necessary to specify a version number by adding an appropriate Git tag to `HEAD` with an
   appropriate prefix.  For example, tagging HEAD with `release/2.3.8` will cause version `2.3.8` to be published on
   the next build.


## Documentation

From the website...
"Commons VFS provides a single API for accessing various different file systems. It presents a uniform view of the files from various different sources, such as the files on local disk, on an HTTP server, or inside a Zip archive."

Now Apache Commons VFS can now add Google Cloud Storage to the list.

The system will use your GCP credentials in the following way

1. `GOOGLE_APPLICATION_CREDENTIALS` environment variable, pointing to a service account key JSON file path.
2. Cloud SDK credentials `gcloud auth application-default login`
3. App Engine standard environment credentials.
4. Compute Engine credentials.

Here is an example using the API:
```java
 String currFileNameStr;

        // Now let's create a temp file just for upload
        File temp = File.createTempFile("uploadFile01", ".tmp");
        try (FileWriter fw = new FileWriter(temp)) {
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append("testing...");
            bw.flush();
        }

        DefaultFileSystemManager currMan = new DefaultFileSystemManager();
        currMan.addProvider("gcs", new GCSFileProvider());
        currMan.addProvider("file", new DefaultLocalFileProvider());
        currMan.init();

        // Create a URL for creating this remote file
        currFileNameStr = "test01.tmp";
        String bucket = "gcp-ltouati-2";
        String currUriStr = String.format("%s://%s/%s",
                                          "gcs", bucket, currFileNameStr);

// Resolve the imaginary file remotely.  So we have a file object
        FileObject currFile = currMan.resolveFile(currUriStr);

// Resolve the local file for upload
        FileObject currFile2 = currMan.resolveFile(
                String.format("file://%s", temp.getAbsolutePath()));

// Use the API to copy from one local file to the remote file
        currFile.copyFrom(currFile2, Selectors.SELECT_SELF);

// Delete the temp we don't need anymore
        temp.delete();
        currFile.delete();
```
