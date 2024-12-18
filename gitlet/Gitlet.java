package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Achieve add, init
 */
public class Gitlet {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        String command = args[0];
        // Special case for 'init' command
        if (command.equals("init")) {
            init();
            return;
        }
        // Create Repository object for all other commands
        Repository repo = new Repository();

        switch (command){
            case "init":
                init();
                break;
            case "add":
                if (args.length < 2) {
                    System.out.println("Please specify a file to add.");
                } else {
                    add(args[1]);
                }
                break;
            case "commit":
                if (args.length < 2 || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message.");
                } else {
                    commit(args[1]);
                }
                break;
            case "branch":
                if (args.length < 2) {
                    System.out.println("Please specify a branch name.");
                } else {
                    repo.branch(args[1]);
                }
                break;
            case "log":
                repo.log();
                break;
            case "merge":
                if (args.length < 2) {
                    System.out.println("Please specify a branch to merge.");
                } else {
                    repo.merge(args[1]);
                }
                break;
            case "checkout":
                if (args.length == 2) {
                    repo.checkoutBranch(args[1]); // Checkout branch
                } else if (args.length == 3 && args[1].equals("--")) {
                    repo.checkoutFile(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    repo.checkoutFile(args[1], args[2]); // Checkout branch from specific commit
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "global-log":
                repo.globalLog();
                break;
            case "find":
                if (args.length < 2) {
                    System.out.println("Please specify a commit message.");
                } else {
                    repo.find(args[1]);
                }
                break;
            case "status":
                repo.status();
                break;
            case "reset":
                if (args.length < 2) {
                    System.out.println("Please specify a commit ID.");
                } else {
                    repo.reset(args[1]);
                }
                break;
            default:
                System.out.println("Invalid command." + command);
        }

    }

    private static void init() {
        // initialize the file
        File gitletDir = new File(".gitlet");
        if (gitletDir.exists()) {
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // create the file dir
        gitletDir.mkdir();
        File blobsDir = Utils.join(gitletDir, "blobs");
        File commitsDir = Utils.join(gitletDir, "commits");
        blobsDir.mkdir();
        commitsDir.mkdir();

        // Create and initialize the repository
        Repository repo = new Repository();
        repo.commit("initial commit"); // Initialize the repository with an empty commit
        System.out.println("Initialized empty Gitlet repository.");
    }

    private static void add(String fileName) {
        Repository repo = new Repository();
        repo.addFile(fileName);
    }

    private static void commit(String message) {
        Repository repo = new Repository();
        repo.commit(message);
    }
}
