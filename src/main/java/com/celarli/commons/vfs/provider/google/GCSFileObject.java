package com.celarli.commons.vfs.provider.google;

import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;
import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


/**
 * This class is in charge of bridging the gap between commons vfs and GCS
 */
public class GCSFileObject extends AbstractFileObject {

    private static final Logger log = LoggerFactory.getLogger(GCSFileObject.class);
    /**
     * The GCS client
     */
    private final Storage storage;
    /**
     * The current blob object
     */
    private Blob currentBlob = null;

    private static Tika tika = new Tika();


    /**
     * Constructor
     *
     * @param name    the file name
     * @param fs      the file system object
     * @param storage the GCS client
     */
    GCSFileObject(@Nonnull AbstractFileName name, @Nonnull GCSFileSystem fs, @Nonnull Storage storage) {

        super(name, fs);
        this.storage = storage;
    }


    /**
     * We must override this method because the parent one throws exception.
     *
     * @param modtime the last modified time.
     * @return true if successfully modified, false otherwise.
     * @throws Exception in case an error happens.
     */
    @Override
    protected boolean doSetLastModifiedTime(long modtime) throws Exception {

        return true;
    }


    @Nonnull
    @Override
    protected FileType doGetType() throws Exception {

        log.debug("Trying to get file type for:" + this.getName());
        GcsFileName fileName = (GcsFileName) this.getName();

        if (fileName != null && fileName.getType() == FileType.FOLDER) {
            return FileType.FOLDER;
        }

        Bucket bucket = this.storage.get(fileName.getBucket());
        if (bucket == null || !bucket.exists()) {
            throw new IllegalArgumentException(format("Bucket %s does not exists", fileName.getBucket()));
        }

        String path = fileName.getPath();

        if (!path.equals("/") && path.startsWith("/")) {
            path = path.substring(1);
        }

        Blob blob = bucket.get(path);

        if (blob != null && blob.exists()) {
            log.debug(format("File :%s exists on bucket", this.getName()));
            return FileType.FILE;
        }
        else {
            // GCS does not have folders.  Just files with path separators in their names.

            // Here's the trick for folders.
            //
            // Do a listing on that prefix.  If it returns anything, after not existing, then it's a folder.
            String url = computePostfix(fileName);
            log.debug(format("File does not :%s exists on bucket try to see if it's a directory", this.getName()));
            Page<Blob> blobs;
            if (url.equals("/")) {
                // Special root path case. List the root blobs with no prefix
                return FileType.FOLDER;
            }
            else {

                //blobs are not listed if url starts with /.
                if (url.startsWith("/")) {
                    url = url.substring(1);
                }

                log.debug(format("listing directory :%s", url));
                blobs = bucket.list(Storage.BlobListOption.currentDirectory(), Storage.BlobListOption.prefix(url));
            }
            if (blobs.getValues().iterator().hasNext()) {
                return FileType.FOLDER;
            }
        }
        return FileType.IMAGINARY;

    }


    @Nonnull
    @Override
    protected String[] doListChildren() throws Exception {

        log.debug(format("Listing directory below:%s", this.getName().toString()));

        GcsFileName fileName = (GcsFileName) this.getName();
        Bucket bucket = this.storage.get(fileName.getBucket());
        if (bucket == null || !bucket.exists()) {
            throw new IllegalArgumentException(format("Bucket %s does not exists", fileName.getBucket()));
        }

        String path = computePostfix(fileName);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Page<Blob> blobs = bucket.list(Storage.BlobListOption.currentDirectory(), Storage.BlobListOption.prefix(path));

        List<String> children = new ArrayList<>();
        for (Blob blob : blobs.iterateAll()) {
            String name = blob.getName();
            if (!name.equalsIgnoreCase(path)) {
                String strippedName = name.substring(path.length());
                children.add(strippedName);
            }
        }
        String[] childrenArray = new String[children.size()];
        children.toArray(childrenArray);

        return childrenArray;
    }


    @Nonnull
    private String computePostfix(@Nonnull GcsFileName fileName) {

        String postfix = fileName.getPath();
        if (!postfix.endsWith("/")) {
            postfix += "/";
        }
        return postfix;
    }


    @Override
    protected long doGetContentSize() throws Exception {

        return this.currentBlob.getSize();
    }


    @Nonnull
    @Override
    protected InputStream doGetInputStream() throws Exception {

        final ReadChannel readChannel = this.storage.reader(this.currentBlob.getBlobId());
        return Channels.newInputStream(readChannel);
    }


    /**
     * Callback for handling create folder requests.  Since there are no folders
     * in GCS this call is ignored.
     *
     * @throws Exception ignored
     */
    @Override
    protected void doCreateFolder() throws Exception {

        log.info("doCreateFolder() called.");
    }


    /**
     * Used for creating folders.  It's not used since GCS does not have
     * the concept of folders.
     *
     * @throws FileSystemException ignored
     */
    @Override
    public void createFolder() throws FileSystemException {

        log.debug("createFolder() called.");
    }


    @Override
    protected void doAttach() throws Exception {

        GcsFileName fileName = (GcsFileName) this.getName();

        Bucket bucket = this.storage.get(fileName.getBucket());

        if (bucket == null || !bucket.exists()) {
            throw new IllegalArgumentException(format("Bucket %s does not exists", fileName.getBucket()));
        }

        String path = fileName.getPath();

        if (!path.equals("/") && path.startsWith("/")) {
            path = path.substring(1);
        }
        this.currentBlob = bucket.get(path);
    }


    @Override
    protected void doDelete() throws Exception {

        getCurrentBlob();
        this.currentBlob.delete();
    }


    @Nonnull
    @Override
    protected OutputStream doGetOutputStream(boolean bAppend) {

        getCurrentBlob(true);
        return Channels.newOutputStream(this.currentBlob.writer());
    }


    private void getCurrentBlob(boolean detectContentType) {

        GcsFileName fileName = (GcsFileName) this.getName();

        String path = fileName.getPath();

        //while deleting files recursively, empty folders are not being deleted if path doesn't ends with /.
        if (fileName != null && fileName.getType() == FileType.FOLDER) {
            path = computePostfix(fileName);
        }

        if (!path.equals("/") && path.startsWith("/")) {
            path = path.substring(1);
        }

        BlobInfo blobInfo;
        if (detectContentType) {
            String baseName = getName().getBaseName();
            String contentType = tika.detect(baseName);

            blobInfo = BlobInfo.newBuilder(fileName.getBucket(), path).setContentType(contentType).build();
        }
        else {
            blobInfo = BlobInfo.newBuilder(fileName.getBucket(), path).build();
        }

        this.currentBlob = storage.create(blobInfo);
    }


    private void getCurrentBlob() {

        getCurrentBlob(false);
    }


    /**
     * Returns the file's list of children.
     *
     * @return The list of children
     * @throws FileSystemException If there was a problem listing children
     * @see AbstractFileObject#getChildren()
     */
    @Override
    public FileObject[] getChildren() throws FileSystemException {

        try {
            // Folders which are copied from other folders, have type = IMAGINARY. We can not throw exception based on folder
            // type only and so we have check here for content.
            if (getType().hasContent()) {
                throw new FileNotFolderException(getName());
            }
        }
        catch (Exception ex) {
            throw new FileNotFolderException(getName(), ex);
        }

        return super.getChildren();
    }


    /**
     * Callback for handling the <code>getLastModifiedTime()</code> Commons VFS API call.
     *
     * @return Time since the file has last been modified
     * @throws Exception
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception {

        if (currentBlob != null) {
            return currentBlob.getUpdateTime();
        }

        return super.doGetLastModifiedTime();
    }


    @Override
    public void copyFrom(FileObject file, FileSelector selector) throws FileSystemException {

        this.copyFrom(file, selector, null);
    }


    /**
     * This method help to copy blob server side if source and destination location belongs to same project.
     * It also takes listener to report progress of file/folder being copied.
     *
     * @param file
     * @param selector
     * @param copyStreamListener
     * @throws FileSystemException
     */
    public void copyFrom(FileObject file, FileSelector selector, CopyStreamListener copyStreamListener)
            throws FileSystemException {

        if (!file.exists()) {
            throw new FileSystemException("vfs.provider/copy-missing-file.error", file);
        }

        if (canCopyServerSide(file)) {
            GcsFileName fileName = (GcsFileName) this.getName();
            String path = fileName.getPath();
            if (!path.equals("/") && path.startsWith("/")) {
                path = path.substring(1);
            }
            String bucket = fileName.getBucket();
            GCSFileObject gcsFile = (GCSFileObject) file;
            CopyWriter copyWriter = gcsFile.currentBlob.copyTo(BlobId.of(bucket, path));
            try {
                //Need to reset file type after copy operation
                this.injectType(this.doGetType());
            }
            catch (Exception e) {
                //swallowed intentionally to continue working further
            }

            //Current blob is now copied one
            this.currentBlob = copyWriter.getResult();
        }
        else {
            copyThroughStream(file, selector, copyStreamListener);

            try {
                //Required to refresh blob once it is copied to get updated metadata of blob, i.e. size
                this.doAttach();
            }
            catch (Exception e) {
                //swallowed intentionally to continue working further
            }
        }
    }


    /**
     * Method copied from AbstractFileObject of Apache VFS lib. With support to report listener for progress.
     *
     * @param file
     * @param selector
     * @param copyStreamListener
     * @throws FileSystemException
     */
    private void copyThroughStream(FileObject file, FileSelector selector, CopyStreamListener copyStreamListener)
            throws FileSystemException {

        // Locate the files to copy across
        final ArrayList<FileObject> files = new ArrayList();
        file.findFiles(selector, false, files);

        // Copy everything across
        final int count = files.size();
        for (int i = 0; i < count; i++) {
            final FileObject srcFile = files.get(i);

            // Determine the destination file
            final String relPath = file.getName().getRelativeName(srcFile.getName());
            final FileObject destFile = resolveFile(relPath, NameScope.DESCENDENT_OR_SELF);

            // Clean up the destination file, if necessary
            if (destFile.exists() && destFile.getType() != srcFile.getType()) {
                // The destination file exists, and is not of the same type, so delete it
                // TODO - add a pluggable policy for deleting and overwriting existing files
                destFile.delete(Selectors.SELECT_ALL);
            }

            // Copy across
            try {
                if (srcFile.getType().hasContent()) {
                    try (InputStream inputStream = srcFile.getContent().getInputStream();
                            OutputStream outputStream = destFile.getContent().getOutputStream()) {

                        Util.copyStream(inputStream, outputStream,
                                Util.DEFAULT_COPY_BUFFER_SIZE, srcFile.getContent().getSize(), copyStreamListener);
                    }
                }
                else if (srcFile.getType().hasChildren()) {
                    destFile.createFolder();
                }
            }
            catch (final IOException e) {
                throw new FileSystemException("vfs.provider/copy-file.error", new Object[] { srcFile, destFile }, e);
            }
        }
    }


    /**
     * Compares credential of source and destination FileObject and return true if they are equals. This helps to copy file
     * directly between buckets for same google storage project
     *
     * @param sourceFileObject
     * @return
     */
    private boolean canCopyServerSide(FileObject sourceFileObject) {

        if (sourceFileObject instanceof GCSFileObject) {
            GCSFileObject gcsFileObject = (GCSFileObject) sourceFileObject;

            Credentials sourceCredential = null;

            if (gcsFileObject.storage != null && gcsFileObject.storage.getOptions() != null) {
                sourceCredential = gcsFileObject.storage.getOptions().getCredentials();
            }

            Credentials destinationCredential = null;
            if (this.storage != null && this.storage.getOptions() != null) {
                destinationCredential = this.storage.getOptions().getCredentials();
            }

            if (sourceCredential != null
                    && destinationCredential != null
                    && sourceCredential.equals(destinationCredential)) {

                return true;
            }
        }

        return false;
    }


    /**
     * Returns false to reply on copyFrom method in case moving/copying file within same google storage project
     *
     * @param fileObject
     * @return
     */
    public boolean canRenameTo(FileObject fileObject) {

        return false;
    }


    /**
     * Generate signed url to directly access file.
     *
     * @param duration - in seconds
     * @return
     * @throws Exception
     */
    public URL signedURL(long duration) throws Exception {

        if (isNull(this.currentBlob)) {
            this.doAttach();
        }

        if (nonNull(this.currentBlob)) {
            return this.currentBlob.signUrl(duration, TimeUnit.SECONDS);
        }

        return null;
    }
}
