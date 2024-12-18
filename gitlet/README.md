# Gitlet: A Mini Version-Control System

## Overview
**Gitlet** is a simplified version-control system inspired by Git. It allows users to track file changes, create branches, merge changes, and manage file versions locally. This project replicates core Git functionalities using Java.

---

## Features

- **Initialization**: Start a new version-control repository.
- **Staging and Committing**: Stage files and save versions using commit messages.
- **Branching and Checking Out**: Create branches, switch between branches, and restore files.
- **Logging**: View commit history for the current branch or all commits globally.
- **Searching**: Find commits with a specific message.
- **Reset and Merge**: Reset to a specific commit and merge branches.
- **Conflict Handling**: Resolve file conflicts during merges.

---

## How to Run Gitlet

### Prerequisites
- Java Development Kit (JDK) installed on your system.
- Java Compiler (`javac`) and Java Runtime Environment (`java`).


### Compilation
Compile all `.java` files in the `gitlet` directory:
```bash
javac gitlet/*.java
java gitlet.Gitlet <command> [operands]
```

