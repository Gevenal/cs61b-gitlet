package gitlet;

import java.io.Serializable;
import java.util.*;

public class StagingArea implements Serializable {
    private Map<String, String> addedFiles; // Track file names to blob hashes
    private Set<String> removedFiles; // Track file names staged for removal

    // Constructor
    public StagingArea () {
        addedFiles = new TreeMap<>();
        removedFiles = new HashSet<>();
    }

    // Stage a file for addition
    public void addFile (String fileName, String blobHash) {
        addedFiles.put(fileName, blobHash);
        removedFiles.remove(fileName);
    }
    // Mark a file for removal
    public void removeFile(String fileName) {
        removedFiles.add(fileName);
        addedFiles.remove(fileName);
    }

    // Clear the staging area
    public void clear() {
        addedFiles.clear();
        removedFiles.clear();
    }

    // Getters for added and removed files
    public Map<String, String> getAddedFiles() {
        return addedFiles;
    }

    public Set<String> getRemovedFiles() {
        return removedFiles;
    }
}
