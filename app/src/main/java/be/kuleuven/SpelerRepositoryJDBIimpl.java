package be.kuleuven;

import java.util.List;
import java.util.List;
import java.util.Map; // Voeg deze import toe


import org.jdbi.v3.core.Jdbi;

public class SpelerRepositoryJDBIimpl implements SpelerRepository {
  private final Jdbi jdbi;

  // Constructor
  SpelerRepositoryJDBIimpl(String connectionString, String user, String pwd) {
    // DONE: vul verder aan of verbeter
    jdbi = Jdbi.create(connectionString, user, pwd);
  }

  @Override
  public void addSpelerToDb(Speler speler) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    jdbi.withHandle(handle -> {
      return handle.execute(
        "INSERT INTO speler (tennisvlaanderenid, naam, punten) VALUES (?, ?, ?)",
        speler.getTennisvlaanderenId(), speler.getNaam(), speler.getPunten()
      );
    });
  }


  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    return jdbi.withHandle(handle -> 
      handle.createQuery("SELECT * FROM speler WHERE tennisvlaanderenid = :id")
            .bind("id", tennisvlaanderenId)
            .mapToBean(Speler.class)
            .findOne()
            .orElseThrow(() -> new InvalidSpelerException("Invalid Speler met identification: " + tennisvlaanderenId))
    );
  }
  
  

  @Override
  public List<Speler> getAllSpelers() {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    return jdbi.withHandle(handle -> {
      return handle.createQuery("SELECT * FROM speler")
        .mapToBean(Speler.class)
        .list();
    });
  }

  @Override
  public void updateSpelerInDb(Speler speler) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    int affectedRows = jdbi.withHandle(handle -> {
      return handle.createUpdate("UPDATE speler SET naam = :naam, punten = :punten WHERE tennisvlaanderenid = :id")
        .bind("naam", speler.getNaam())
        .bind("punten", speler.getPunten())
        .bind("id", speler.getTennisvlaanderenId()) // Hier bind je de juiste parameter
        .execute();
    });
    
    if (affectedRows == 0) {
      throw new InvalidSpelerException("Invalid Speler met identification: " + speler.getTennisvlaanderenId());
    }
    
  }
  
  

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenId) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    int affectedRows = jdbi.withHandle(handle -> 
      handle.createUpdate("DELETE FROM speler WHERE tennisvlaanderenid = :id")
            .bind("id", tennisvlaanderenId)
            .execute()
    );
    if (affectedRows == 0) {
      throw new InvalidSpelerException("Invalid Speler met identification: " + tennisvlaanderenId);
    }    
  }
  

  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenid) {
  // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
      final String[] besteTornooi = {null}; // Use an array to make it mutable
      final String[] besteFase = {null};    // Use an array to make it mutable
  
      // Gebruik de JDBI handle om de query uit te voeren
      return jdbi.withHandle(handle -> {
          // Definieer de query die je wilt uitvoeren
          String query = "SELECT w.finale, w.winnaar, t.clubnaam " +
                         "FROM wedstrijd w " +
                         "JOIN tornooi t ON w.tornooi = t.id " +
                         "WHERE w.speler1 = :id OR w.speler2 = :id";
          
          // Voer de query uit en krijg de resultaten
          List<Map<String, Object>> results = handle.createQuery(query)
              .bind("id", tennisvlaanderenid)
              .mapToMap()
              .list();
  
          int hoogsteScore = -1;
  
          // Verwerk de resultaten
          for (Map<String, Object> row : results) {
              int finale = (int) row.get("finale");
              int winnaar = (int) row.get("winnaar");
              String clubnaam = (String) row.get("clubnaam");
  
              // Bereken de score op basis van de finale
              int score = switch (finale) {
                  case 1 -> (winnaar == tennisvlaanderenid) ? 3 : 2; // Winst of finale
                  case 2 -> 1;  // Halve finale
                  default -> 0; // Geen resultaat
              };
  
              // Als deze score hoger is dan de vorige hoogste score, update dan
              if (score > hoogsteScore) {
                  hoogsteScore = score;
                  besteTornooi[0] = clubnaam;
                  besteFase[0] = switch (score) {
                      case 3 -> "winst";
                      case 2 -> "finale";
                      case 1 -> "halve finale";
                      default -> null;
                  };
              }
          }
  
          // Controleer of er geen resultaten zijn en gooi een fout
          if (besteTornooi[0] == null || besteFase[0] == null) {
              throw new InvalidSpelerException("Geen resultaten voor speler met ID: " + tennisvlaanderenid);
          }
  
          // Return het resultaat
          return "Hoogst geplaatst in het tornooi van " + besteTornooi[0] + " met plaats in de " + besteFase[0];
      });
  }
  
  
  


  @Override
  public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    jdbi.withHandle(handle -> {
      return handle.execute(
        "INSERT INTO speler_speelt_tornooi (speler, tornooi) VALUES (?, ?)",
        tennisvlaanderenId, tornooiId
      );
    });
  }

  @Override
  public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    jdbi.withHandle(handle -> {
      return handle.execute(
        "DELETE FROM speler_speelt_tornooi WHERE speler = ? AND tornooi = ?",
        tennisvlaanderenId, tornooiId
      );
    });
  }
}
