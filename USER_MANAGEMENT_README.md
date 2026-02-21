# BloodLink User Management Dashboard

A JavaFX-based CRUD desktop application for managing users in the BloodLink network.

## Features

- **View All Users**: Display all registered users in an elegant card-based layout
- **Add New Users**: Register new donors or hospital staff members
- **Edit Users**: Modify existing user information
- **Delete Users**: Remove users from the system with confirmation
- **Search & Filter**: Search users by name, email, or phone; filter by user type
- **Real-time Stats**: View total users, donors count, and hospital staff count

## Prerequisites

- Java 8 or higher
- JavaFX SDK (if not included in your JDK)
- PostgreSQL database connection (configured in DataSource.java)

## Project Structure

```
src/tn/edu/esprit/
├── entities/
│   ├── Users.java
│   └── UserType.java
├── services/
│   ├── ServiceUser.java
│   └── IService.java
├── Tools/
│   └── DataSource.java
└── gui/
    ├── UserManagementApp.java        # Main application entry point
    ├── UserDashboard.fxml             # Main dashboard layout
    ├── UserDashboardController.java   # Dashboard controller
    ├── UserDialog.fxml                # Add/Edit user dialog
    ├── UserDialogController.java      # Dialog controller
    └── styles.css                     # Application styles
```

## Running the Application

### Option 1: Using IDE (IntelliJ IDEA / Eclipse)

1. Ensure JavaFX is properly configured in your project
2. Right-click on `UserManagementApp.java`
3. Select "Run 'UserManagementApp.main()'"

### Option 2: Using Command Line

```bash
# Navigate to the project directory
cd "C:\Users\AMINA\BloodLink"

# Compile (ensure JavaFX is in the classpath)
javac --module-path "path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml -d bin src/tn/edu/esprit/**/*.java

# Run
java --module-path "path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml -cp bin tn.edu.esprit.gui.UserManagementApp
```

### Option 3: Using module-info.java (Recommended for Java 11+)

Create a `module-info.java` file at the root of your source folder:

```java
module bloodlink {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    
    opens tn.edu.esprit.gui to javafx.fxml;
    exports tn.edu.esprit.gui;
}
```

## Database Configuration

Make sure your database connection is properly configured in `DataSource.java`:

```java
private String url = "jdbc:postgresql://your-host:5432/your-database";
private String user = "your-username";
private String password = "your-password";
```

## User Types

- **DONOR**: Regular blood donors
- **HOSPITAL_STAFF**: Hospital staff members with administrative access

## Design Features

- Modern dark sidebar navigation
- Clean, card-based user display
- Responsive search and filtering
- Color-coded user type badges (Green for Donors, Purple for Hospital Staff)
- Smooth hover effects and transitions
- Professional form dialogs for add/edit operations

## Troubleshooting

### JavaFX Not Found

If you get "Error: JavaFX runtime components are missing":

1. Download JavaFX SDK from https://openjfx.io/
2. Add VM options to your run configuration:
   ```
   --module-path "path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml
   ```

### Database Connection Issues

- Verify your database is running
- Check credentials in `DataSource.java`
- Ensure PostgreSQL JDBC driver is in your classpath

### FXML loading errors

- Ensure all FXML files are in the same package as their controllers
- Check that fx:controller attributes match the fully qualified class names

## Future Enhancements

- User authentication and login system
- Role-based access control
- Advanced filtering and sorting options
- Export user data to CSV/PDF
- User activity logging
- Profile pictures support
- Email notifications
- Pagination for large datasets

## License

This project is part of the BloodLink blood bank management system.
