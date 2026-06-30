# Residence Management System

A desktop application built in Java (Swing) for managing a student residence. Students can log in to report maintenance issues for their room, and management/admin can log in to view and track those reports. Built to understand how a residence management system works as part of my Computer Science studies.

## Features

- **Login screen** for two types of user: students and management (admin)
- **Student:** report a maintenance issue (e.g. broken light, leaking pipe, door lock fault, no hot water) for their block and room
- **Admin:** view all reported issues and their status
- **Campus model:** blocks (e.g. Block A, Block B) each containing rooms

## Tech Stack

- **Language:** Java (Swing GUI)
- **Build tool:** Maven
- **Data:** in-memory (uses Java collections; no external database needed to run)

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or later
- Apache Maven

### Running the project
1. Clone the repository:
   ```bash
   git clone https://github.com/madiamunzhedzi/ResidenceManagementSystem.git
   ```
2. Move into the project folder and run it with Maven:
   ```bash
   cd ResidenceManagement
   mvn compile exec:java -Dexec.mainClass="com.res.App"
   ```
   *(Or open the project in your IDE and run the `App` class.)*

## Demo Logins

The app ships with sample data for testing. Use these to try it out:

| Role     | Username / ID | PIN / Password |
| -------- | ------------- | -------------- |
| Student  | `212345`      | `1234`         |
| Admin    | `admin`       | `adminpin`     |

> These are test credentials only, used to demonstrate the app.

## What I Learned

This project taught me how to build a multi-screen Java Swing interface, manage different user roles from a single login, and model data (students, blocks, rooms, and maintenance reports) using Java collections.

## Future Improvements

- [ ] Save data to a real database (e.g. MySQL) so it persists between runs
- [ ] Hash and store passwords securely instead of keeping them in plain text
- [ ] Add the ability for admin to update an issue's status (e.g. "in progress", "resolved")
