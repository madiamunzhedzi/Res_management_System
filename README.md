# Residence Management System

A desktop application built in Java (Swing) for managing a student residence. Students can log in to report maintenance issues for their room, and management/admin can log in to view and track those reports. Built to understand how a residence management system works as part of my Computer Science studies.

## Features

- **Login screen** for two types of user: students and management (admin)
- **Student:** report a maintenance issue (e.g. broken light, leaking pipe, door lock fault, no hot water) for their block and room
- **Admin:** view all reported issues, mark them as **Resolved**, and manage students, the campus map (blocks and rooms) and the list of issue types
- **Campus model:** blocks (e.g. Block A, Block B) each containing rooms
- **Saved data:** all students, reports, blocks, rooms and issue types are saved to disk and reloaded automatically, so nothing is lost when the app closes
- **Input validation:** the app checks for empty/duplicate entries and always tells the user whether an action succeeded or why it failed

## Tech Stack

- **Language:** Java (Swing GUI)
- **Build tool:** Maven
- **Data:** persisted to a local flat file using only the standard Java library (no external database or dependencies needed to run)

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or later
- Apache Maven

### Running the project
1. Clone the repository:
   ```bash
   git clone https://github.com/madiamunzhedzi/Res_management_System.git
   ```
2. Move into the project folder and run it with Maven:
   ```bash
   cd ResidenceManagement
   mvn compile exec:java -Dexec.mainClass="com.res.App"
   ```
   *(Or open the project in your IDE and run the `App` class.)*

### Where the data is stored
On first run the app creates a `data/resdata.txt` file inside the `ResidenceManagement` folder (the directory you run it from) and seeds it with the sample data below. Edits made in the app are written back to this file. Deleting the file resets the app to the sample data on the next launch. The `data/` folder is git-ignored so your local data is never committed.

## Demo Logins

The app ships with sample data for testing. Use these to try it out:

| Role     | Username / ID | PIN / Password |
| -------- | ------------- | -------------- |
| Student  | `212345`      | `1234`         |
| Admin    | `admin`       | `adminpin`     |

> These are test credentials only, used to demonstrate the app.

## What I Learned

This project taught me how to build a multi-screen Java Swing interface, manage different user roles from a single login, model data (students, blocks, rooms, and maintenance reports) using Java collections, persist that data to disk safely (with escaping and atomic writes), and validate user input so the program behaves predictably.

## Future Improvements

- [x] Save data so it persists between runs *(now saved to a local file)*
- [x] Let admin update an issue's status *(admin can now mark reports as Resolved)*
- [ ] Move from the flat file to a real database (e.g. MySQL or SQLite)
- [ ] Hash and store passwords securely instead of keeping them in plain text
