package be.kuleuven;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class ConnectionManager {
  private String connectionString;
  private Connection connection;

  public ConnectionManager(String connectionString, String user, String pwd) {
    try {
      this.connectionString = connectionString;
      this.connection = DriverManager.getConnection(connectionString, user, pwd);
      connection.setAutoCommit(false); // Uitschakelen van auto-commit voor gecontroleerde transacties
    } catch (SQLException e) {
      System.out.println("Error connecting to database with connectionstring: " + connectionString + ", and user: "
          + user + ", and the given password.");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public String getConnectionString() {
    return connectionString;
  }

  public void flushConnection() {
    try {
      connection.commit(); // Zorg ervoor dat de commit echt plaatsvindt
      connection.close(); // Sluit de verbinding pas na commit
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void initTables() {
    try {
      URI path = Objects.requireNonNull(App.class.getClassLoader().getResource("initTableWithDummyData.sql")).toURI();
      var sql = new String(Files.readAllBytes(Paths.get(path)));
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(sql); // Voer de SQL uit voor het initialiseren van de tabellen
        connection.commit(); // Commit de veranderingen naar de database
      }
    } catch (Exception e) {
      System.out.println("An Error occurred when trying to initialize database table");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void verifyTableContentOfInit() {
    try (Statement statement = connection.createStatement()) {
      var result = statement.executeQuery("SELECT COUNT(*) as cnt FROM speler;");
      while (result.next()) {
        assert result.getInt("cnt") == 8; // Controleer of het aantal rijen overeenkomt met de verwachte waarde
      }
    } catch (AssertionError a) {
      System.out.println("The assertion of #rows == 8 failed");
      a.printStackTrace();
      throw new RuntimeException(a);
    } catch (Exception e) {
      System.out.println("Error when trying to verify initialized table");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
