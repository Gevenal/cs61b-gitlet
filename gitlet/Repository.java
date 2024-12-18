package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author xxr
 */
public class Repository {
    private StagingArea stagingArea;
    private String head; // Points to the current commit ID
    private Map<String, String> branches; // Maps branch names to commit IDs
    private String currentBranch; // Tracks the current branch
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File BLOBS_DIR = Utils.join(GITLET_DIR, "blobs");
    public static final File COMMITS_DIR = Utils.join(GITLET_DIR, "commits");
    public static final File HEAD_FILE = Utils.join(GITLET_DIR, "HEAD");
    private static final File STAGING_AREA_FILE = Utils.join(GITLET_DIR, "staging");
    private static final File BRANCHES_FILE = Utils.join(GITLET_DIR, "branches");

    public Repository() {

        if (!GITLET_DIR.exists()) {
            throw new IllegalStateException("Not in an initialized Gitlet directory.");
        }
        // Initialize branches map
        if (BRANCHES_FILE.exists()) {
            @SuppressWarnings("unchecked")
            Map<String, String> loadedBranches = Utils.readObject(BRANCHES_FILE, TreeMap.class);
            this.branches = loadedBranches;
        } else {
            this.branches = new TreeMap<>(); // Initialize branches to avoid null
        }
        // Initializing staging area
        if (STAGING_AREA_FILE.exists()) {
            stagingArea = Utils.readObject(STAGING_AREA_FILE, StagingArea.class);
        } else {
            stagingArea = new StagingArea();
        }
        // Ensure required directories exist
        if (!COMMITS_DIR.exists()) {
            COMMITS_DIR.mkdir();
        }
        if (!BLOBS_DIR.exists()) {
            BLOBS_DIR.mkdir();
        }
        // Initialize HEAD and current branch
        if (HEAD_FILE.exists()) {
            this.currentBranch = Utils.readContentsAsString(HEAD_FILE);
            this.head = branches.get(currentBranch);
        } else {
            this.currentBranch = "main";
            this.head = null;

            branches.put(currentBranch, null); // Add 'main' branch to branches map
            saveBranches();               // Save branches map to BRANCHES_FILE
            saveHeadAndBranch();          // Save currentBranch to HEAD_FILE
        }

    }

    /**
     * Check if the file exists.
     * Read the file content and create a Blob.
     * Compare the file's hash with the latest commit’s version.
     * Add the file to the staging area if it’s new or modified.
     */
    public void addFile(String fileName) {
        File file = Utils.join(CWD, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist");
            return;
        }

        String content = Utils.readContentsAsString(file);
        Blob blob = new Blob(fileName, content); // Create a blob
        String hash = blob.getHash();

        File blobFile = Utils.join(BLOBS_DIR, hash);
        if (!blobFile.exists()) {
            blob.saveBlob();
        }

        stagingArea.addFile(fileName, hash);
        Utils.writeObject(STAGING_AREA_FILE, stagingArea); // Save staging area
        System.out.println("File staged for addition: " + fileName);
      //  System.out.println("Staging Area after add: " + stagingArea.getAddedFiles());
    }

    // Commit changes
    public void commit (String message) {
        if (stagingArea.getAddedFiles().isEmpty() && stagingArea.getRemovedFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        // Load the parent commit if it exists
        Commit parentCommit = head == null ? null: Commit.loadCommit(head);

        // Build the filePointers map for the new commit
        Map<String, String> filePointers = new TreeMap<>();
        if (parentCommit != null) {
            filePointers.putAll(parentCommit.getFilePointers()); // Start with parent's file pointers
        }
        filePointers.putAll(stagingArea.getAddedFiles()); // Add staged files
        for (String removedFile: stagingArea.getRemovedFiles()) {
            filePointers.remove(removedFile);  // Remove files staged for removal
        }

        // Debugging: Log filePointers before creating the commit
      //  System.out.println("File pointers for new commit: " + filePointers);

        // Create the new commit and save
        Commit newCommit = new Commit(message, head, filePointers);
        newCommit.saveCommit();

        // Update HEAD
        //Utils.writeContents(HEAD_FILE, newCommit.getID());
        branches.put(currentBranch, newCommit.getID()); // Update the current branch pointer
        saveBranches();
        saveHeadAndBranch();

        stagingArea.clear();
        Utils.writeObject(STAGING_AREA_FILE, stagingArea);

     //  System.out.println("Commited with ID: " + newCommit.getID());
    }

    public void branch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        if (head == null) {
            System.out.println("No commits exist yet. Cannot create a branch.");
            return;
        }
        branches.put(branchName, head);
        saveBranches();
    }

    private void saveBranches() {
        Utils.writeObject(BRANCHES_FILE,(Serializable) branches);
    }
    // checkout -- [file-name]: Restore a file from the current commit
    public void checkoutFile(String fileName) {
        if (head == null) {
            System.out.println("No commits exist yet.");
            return;
        }

        Commit currentCommit = Commit.loadCommit(head);
        String blobHash = currentCommit.getFilePointers().get(fileName);
        if (blobHash == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File blobFile = Utils.join(BLOBS_DIR, blobHash);
        Utils.writeContents(Utils.join(CWD, fileName), Utils.readContents(blobFile));
    }

    //checkout [commit-id] -- [file-name]: Restore a file from a specific commit
    public void checkoutFile(String commitID, String fileName) {
        Commit commit = Commit.loadCommit(commitID);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String blobHash = commit.getFilePointers().get(fileName);
        if (blobHash == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File blobFile = Utils.join(BLOBS_DIR, blobHash);
        if (!blobFile.exists()) {
            System.out.println("Blob file does not exist " + blobHash);
            return;
        }
        Utils.writeContents(Utils.join(CWD, fileName), Utils.readContents(blobFile));
    }

    // checkout [branch-name]
    public void checkoutBranch (String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String branchHead = branches.get(branchName);
        if (branchHead == null) {
            System.out.println("Branch has no commits yet.");
            return;
        }

        Commit branchCommit = Commit.loadCommit(branchHead);
        Utils.plainFilenamesIn(CWD).forEach(fileName -> Utils.restrictedDelete(Utils.join(CWD, fileName)));
        branchCommit.getFilePointers().forEach((fileName, blobHash) -> {
            File blobFile = Utils.join(BLOBS_DIR, blobHash);
            Utils.writeContents(Utils.join(CWD, fileName), Utils.readContents(blobFile));
        });

        // Update head and current branch
        currentBranch = branchName;
        head = branchHead;
        saveHeadAndBranch();

        stagingArea.clear();
        Utils.writeObject(STAGING_AREA_FILE, stagingArea);
    }

    private void saveHeadAndBranch() {
        Utils.writeContents(HEAD_FILE, currentBranch);
        saveBranches();
    }

    // displays a history of commits starting from the current branch's
    public void log() {
        if (head == null) {
            System.out.println("No commits exist yet.");
            return;
        }

        Commit currentCommit = Commit.loadCommit(head);
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommit.getID());
            System.out.println("Date: " + currentCommit.getTimeStamp());
            System.out.println(currentCommit.getMessage());
            System.out.println();

            // Move to the parent commit
            String parentID = currentCommit.getParent();
            currentCommit = parentID != null ? Commit.loadCommit(parentID) : null;
        }
    }

    public void merge(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        if (!stagingArea.getAddedFiles().isEmpty() || !stagingArea.getRemovedFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        String branchHead = branches.get(branchName);
        if (branchHead == null) {
            System.out.println("Branch has no commits.");
            return;
        }

        // Step 2: Find the Split Point
        String otherBranchHead = branches.get(branchName);
        String splitPointID = findSplitPoint(otherBranchHead);
        if (splitPointID == null) {
            System.out.println("No split point found.");
            return;
        }

        Commit splitCommit = Commit.loadCommit(splitPointID);
        Commit currentCommit = Commit.loadCommit(head);
        Commit otherCommit = Commit.loadCommit(otherBranchHead);

        // Step 3: Compare and Merge Files
        Map<String, String> splitFiles = splitCommit.getFilePointers();
        Map<String, String> currentFiles = currentCommit.getFilePointers();
        Map<String, String> otherFiles = otherCommit.getFilePointers();

        for (String fileName : otherFiles.keySet()) {
            String splitHash = splitFiles.get(fileName);
            String currentHash = currentFiles.get(fileName);
            String otherHash = otherFiles.get(fileName);

            if (splitHash == null) {
                // File added in other branch but not in split point
                checkoutFile(otherBranchHead, fileName);
                stagingArea.addFile(fileName, otherHash);
            } else if (Objects.equals(currentHash, splitHash) && !Objects.equals(otherHash, splitHash)) {
                // File modified in other branch only
                checkoutFile(otherBranchHead, fileName);
                stagingArea.addFile(fileName, otherHash);
            } else if (!Objects.equals(currentHash, splitHash) && !Objects.equals(otherHash, splitHash)) {
                // Conflict: Modified in both branches
                handleConflict(fileName, currentHash, otherHash);
            }
        }

        // Step 4: Create Merge Commit
        String mergeMessage = "Merged " + branchName + " into " + currentBranch + ".";
        Commit mergeCommit = new Commit(mergeMessage, head, currentCommit.getFilePointers());
        mergeCommit.saveCommit();
        head = mergeCommit.getID();
        branches.put(currentBranch, head);
        saveBranches();
        saveHeadAndBranch();

        System.out.println("Merge successful.");
    }

    private void handleConflict(String fileName, String currentHash, String otherHash) {
        String currentContent = currentHash != null ? Utils.readContentsAsString(Utils.join(BLOBS_DIR, currentHash)) : "";
        String otherContent = otherHash != null ? Utils.readContentsAsString(Utils.join(BLOBS_DIR, otherHash)) : "";

        String conflictContent = "<<<<<<< HEAD\n" + currentContent + "=======\n" + otherContent + ">>>>>>>\n";
        File file = Utils.join(CWD, fileName);
        Utils.writeContents(file, conflictContent);

        System.out.println("Encountered a merge conflict in " + fileName);
    }

    private String findSplitPoint(String branchHead) {
        // Use two sets to track visited commits
        Set<String> currentBranchCommits = new HashSet<>();
        Set<String> otherBranchCommits = new HashSet<>();

        // Traverse the current branch to collect all ancestor commits
        String currentCommitId = head;
        while (currentCommitId != null) {
            currentBranchCommits.add(currentCommitId);
            Commit commit = Commit.loadCommit(currentCommitId);
            currentCommitId = commit.getParent();
        }
        // Traverse the given branch and check for a common ancestor
        String otherCommitId = branchHead;
        while (otherCommitId != null) {
        if (currentBranchCommits.contains(otherCommitId)) {
            return otherCommitId; // Found the split points
        }
        otherBranchCommits.add(otherCommitId);
        Commit commit = Commit.loadCommit(otherCommitId);
        otherCommitId = commit.getParent();
    }
        return null;
    }

    public void globalLog() {
        List<String> commitFiles = Utils.plainFilenamesIn(COMMITS_DIR);
        if (commitFiles == null) return;

        for (String commitID : commitFiles) {
            Commit commit = Commit.loadCommit(commitID);
            System.out.println("===");
            System.out.println("commit " + commit.getID());
            System.out.println("Date: " + commit.getTimeStamp());
            System.out.println(commit.getMessage());
            System.out.println();
        }
    }

    public void find(String message) {
        List<String> commitFiles = Utils.plainFilenamesIn(COMMITS_DIR);
        if (commitFiles == null) return;

        boolean found = false;
        for (String commitID : commitFiles) {
            Commit commit = Commit.loadCommit(commitID);
            if (commit.getMessage().equals(message)) {
                System.out.println(commitID);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        for (String branch : branches.keySet()) {
            if (branch.equals(currentBranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }

        System.out.println("\n=== Staged Files ===");
        for (String file : stagingArea.getAddedFiles().keySet()) {
            System.out.println(file);
        }

        System.out.println("\n=== Removed Files ===");
        for (String file : stagingArea.getRemovedFiles()) {
            System.out.println(file);
        }
    }

    public void reset(String commitID) {
        File commitFile = Utils.join(COMMITS_DIR, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit targetCommit = Commit.loadCommit(commitID);

        // Clear current working directory
        Utils.plainFilenamesIn(CWD).forEach(fileName -> Utils.restrictedDelete(Utils.join(CWD, fileName)));

        // Restore files from the target commit
        targetCommit.getFilePointers().forEach((fileName, blobHash) -> {
            File blobFile = Utils.join(BLOBS_DIR, blobHash);
            Utils.writeContents(Utils.join(CWD, fileName), Utils.readContents(blobFile));
        });

        // Update head to point to the target commit
        head = commitID;
        branches.put(currentBranch, head);
        saveHeadAndBranch();

        stagingArea.clear();
        Utils.writeObject(STAGING_AREA_FILE, stagingArea);
    }



}
