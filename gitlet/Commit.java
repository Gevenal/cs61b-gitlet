package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    private String message;   // Commit message
    private String timeStamp;   // Commit timestamp
    private String parent;
    private Map<String, String> filePointers; // Maps file names to blob hashes
    private String id;

    public Commit(String message, String parent, Map<String, String> filePointers) {
      //  System.out.println("Debug - Commit constructor called");
      //  System.out.flush();
        this.message = message;
        this.parent = parent;
        this.filePointers = new TreeMap<>(filePointers);

        if (parent == null) {
            this.timeStamp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date(0)); // Epoch time for the initial commit
        } else {
            this.timeStamp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date());
        }

        // Compute the unique ID using a SHA-1 hash of the commit data
        this.id = Utils.sha1(message, timeStamp, parent == null ? "": parent, serializeFilePointers(filePointers));
    }

    // Helper method to serialize filePointers deterministically
    private String serializeFilePointers(Map<String, String> filePointers) {
        if (filePointers.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry: filePointers.entrySet()) {
            String fileName = entry.getKey();
            String blobHash = entry.getValue();
            builder.append(fileName).append(":").append(blobHash).append(";");
        }
        return builder.toString();
    }

    // Save the commit to the .gitlet/commits directory
    public void saveCommit() {
        File commitFile = Utils.join(Repository.COMMITS_DIR, id);
        Utils.writeObject(commitFile, this);
    }

    // Static method to load a commit by ID
    public static Commit loadCommit(String commitID) {
        File commitFile = Utils.join(Repository.COMMITS_DIR, commitID);
        return Utils.readObject(commitFile, Commit.class);
    }

    public String getID() {
        return id;
    }

    public String getParent() {
        return this.parent;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public Map<String, String> getFilePointers() {
        return filePointers;
    }

}
