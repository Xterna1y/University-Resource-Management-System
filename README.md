# Campus MCP Assignment — Supplied Code

This package contains two Maven modules:

| Module | What it is | Who touches it |
|--------|------------|----------------|
| `campus-info-mcp-server` | The **supplied** Campus Information MCP server (HTTP/SSE). Tools, Resources, Prompts, Capabilities. | **Do not modify.** Students connect to it. |
| `reference-javafx-client` | A **reference** JavaFX MCP client showing discovery, RAG and a direct tool call. | A starting point. Extend into your own app. |

> **RESTRICTIONS (read the assignment brief):** no Spring, no Quarkus, no database. The
> knowledge base and all stored data are plain **text files**. Jetty is used only as a small
> servlet host for the server's SSE endpoint — it is not an application framework.

## Prerequisites
- JDK 25, Maven 3.9+
- An Anthropic API key for the RAG features: `export ANTHROPIC_API_KEY="sk-ant-..."`

## 1. Build everything
```bash
mvn clean package
```

## 2. Start the MCP server (terminal 1)
```bash
java -jar campus-info-mcp-server/target/campus-info-mcp-server.jar
# SSE stream:   http://localhost:8080/sse
# Message POST: http://localhost:8080/mcp/message
```

## 3. Run the reference client (terminal 2)
```bash
mvn -pl reference-javafx-client -am javafx:run
```
If you get an error, then just change directory to the client path and then run the command
```bash
cd .\reference-javafx-client\

mvn compile javafx:run
```
The client connects to `http://localhost:8080`, lists the server's capabilities, and lets you
ask grounded questions (RAG) and book a room (direct tool call).

See the Word guides shipped with the assignment for full explanations and architecture diagrams.

```
# Campus Companion

Campus Companion is a JavaFX client application that connects to the supplied Campus Information MCP Server. It allows students to browse campus information, search facilities, manage bookings, and interact with an AI-powered campus assistant.

---

## Features

- Dashboard
- Campus Assistant (RAG-powered)
- Browse Campus Facilities
- Facility Booking
- Booking History
```
---

## Running the Application

### 1. Start the MCP Server

```
run the CampusMcpServer.java file
```

### 2. Start the JavaFX Client

```
run the Launcher.java file
```

---

## How to Use

### Dashboard
Provides quick access to all major features.

### Facilities

1. Browse or search for a facility.
2. Select a facility.
3. Click **Check Availability**.
4. Choose a booking date. 
5. Click **Check Availability**. 
6. Click **Continue to Booking**. 
7. Select a time slot. 
8. Enter the number of attendees.
9. Confirm the booking.

### Bookings

- View all existing bookings.
- Cancel a booking if required.

### Assistant

Ask questions about:

- Campus facilities
- University policies
- General campus information
- Booking-related information

---

## Notes for the Marker

- This project uses the supplied MCP Server without modification.
- All booking data and knowledge base information are stored in text files as required by the assignment.
- No database or Spring framework is used.
- The client communicates with the server using MCP Tools, Resources and Prompts.

---
