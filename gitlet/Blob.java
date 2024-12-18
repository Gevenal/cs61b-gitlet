package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.MessageDigest;

public class Blob implements Serializable {
    private String fileName;
    private String fileContent;
    private String hash;

    public Blob(String fileName, String fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.hash = Utils.sha1(fileContent);
    }

    // Save the blob to the .gitlet/blobs dir
    public void saveBlob() {
        File blobFile = Utils.join(Repository.BLOBS_DIR, hash);
        Utils.writeContents(blobFile, fileContent);

    }

    public String getHash() {
        return hash;
    }
}
