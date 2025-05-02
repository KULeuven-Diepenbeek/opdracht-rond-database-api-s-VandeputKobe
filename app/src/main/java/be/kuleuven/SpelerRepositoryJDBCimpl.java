package be.kuleuven;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import org.checkerframework.checker.units.qual.s;

public class SpelerRepositoryJDBCimpl implements SpelerRepository {
  private Connection connection;

  // Constructor
  SpelerRepositoryJDBCimpl(Connection connection) {
    this.connection = connection;
    // DONE: vul contructor verder aan
  }

  @Override
  public void addSpelerToDb(Speler speler) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    try{
      PreparedStatement spelerStmt = connection.prepareStatement(
      "INSERT INTO speler (tennisvlaanderenid, naam, punten) VALUES (?, ?, ?)");
      spelerStmt.setInt(1, speler.getTennisvlaanderenId());
      spelerStmt.setString(2, speler.getNaam());
      spelerStmt.setInt(3, speler.getPunten());
      spelerStmt.executeUpdate();
      spelerStmt.close();

      PreparedStatement tornooiStmt = connection.prepareStatement(
      "INSERT INTO speler_speelt_tornooi (speler, tornooi) VALUES (?, ?)");

      for (Tornooi t : speler.getTornooien()) {
        tornooiStmt.setInt(1, speler.getTennisvlaanderenId());
        tornooiStmt.setInt(2, t.getId());
        tornooiStmt.addBatch();
      }
      tornooiStmt.executeBatch();
      tornooiStmt.close();

      PreparedStatement wedstrijdStmt = connection.prepareStatement(
  "INSERT INTO wedstrijd (tornooi, speler1, speler2, winnaar, score, finale) VALUES (?, ?, ?, ?, ?, ?)");

  for (Wedstrijd wedstrijd : speler.getWedstrijden()) {
    // Reuse the already declared wedstrijdStmt

    // Stel de parameters in voor de wedstrijd
    wedstrijdStmt.setInt(1, wedstrijd.getTornooiId()); // tornooi ID
    wedstrijdStmt.setInt(2, wedstrijd.getSpeler1Id()); // speler1 ID
    wedstrijdStmt.setInt(3, wedstrijd.getSpeler2Id()); // speler2 ID
    // Als winnaar null is, zetten we de waarde naar null in de DB
    if (wedstrijd.getWinnaarId() != 0) {
        wedstrijdStmt.setInt(4, wedstrijd.getWinnaarId());
    } else {
        wedstrijdStmt.setNull(4, java.sql.Types.INTEGER);
    }
    wedstrijdStmt.setString(5, wedstrijd.getScore()); // score
    wedstrijdStmt.setInt(6, wedstrijd.getFinale()); // finale status
    wedstrijdStmt.executeUpdate();
    wedstrijdStmt.close();
}

    // Commit de veranderingen naar de database
    connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
  //DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    Speler found_speler = null;

    try {
        // Haal speler op uit de speler tabel
        String spelerQuery = "SELECT * FROM speler WHERE tennisvlaanderenid = ?";
        PreparedStatement spelerStmt = connection.prepareStatement(spelerQuery);
        spelerStmt.setInt(1, tennisvlaanderenId);
        ResultSet spelerResult = spelerStmt.executeQuery();

        if (spelerResult.next()) {
            int id = spelerResult.getInt("tennisvlaanderenid");
            int punten = spelerResult.getInt("punten");
            String naam = spelerResult.getString("naam");

            // Maak de Speler object aan
            found_speler = new Speler(id, naam, punten);
        } else {
            // Speler niet gevonden, gooi een foutmelding
            throw new InvalidSpelerException("Speler met ID " + tennisvlaanderenId + " niet gevonden");
        }

        // Haal de tornooien op voor de gevonden speler
        String tornooiQuery = "SELECT * FROM speler_speelt_tornooi WHERE speler = ?";
        PreparedStatement tornooiStmt = connection.prepareStatement(tornooiQuery);
        tornooiStmt.setInt(1, tennisvlaanderenId);
        ResultSet tornooiResult = tornooiStmt.executeQuery();

        while (tornooiResult.next()) {
            int tornooiId = tornooiResult.getInt("tornooi");

            // Haal de tornooi gegevens op binnen de while loop
            String tornooiDetailsQuery = "SELECT * FROM tornooi WHERE id = ?";
            PreparedStatement tornooiDetailsStmt = connection.prepareStatement(tornooiDetailsQuery);
            tornooiDetailsStmt.setInt(1, tornooiId);
            ResultSet tornooiDetailsResult = tornooiDetailsStmt.executeQuery();

            if (tornooiDetailsResult.next()) {
                String clubnaam = tornooiDetailsResult.getString("clubnaam");

                // Maak een Tornooi object aan en voeg het toe aan de speler
                Tornooi tornooi = new Tornooi(tornooiId, clubnaam);
                found_speler.addTornooi(tornooi);
            }

            tornooiDetailsResult.close();
            tornooiDetailsStmt.close();
        }

        // Haal de wedstrijden op voor de speler
        String wedstrijdQuery = "SELECT * FROM wedstrijd WHERE speler1 = ? OR speler2 = ?";
        PreparedStatement wedstrijdStmt = connection.prepareStatement(wedstrijdQuery);
        wedstrijdStmt.setInt(1, tennisvlaanderenId);
        wedstrijdStmt.setInt(2, tennisvlaanderenId);
        ResultSet wedstrijdResult = wedstrijdStmt.executeQuery();

        while (wedstrijdResult.next()) {
            int wedstrijdId = wedstrijdResult.getInt("id");
            int tornooiId = wedstrijdResult.getInt("tornooi");
            int speler1Id = wedstrijdResult.getInt("speler1");
            int speler2Id = wedstrijdResult.getInt("speler2");
            Integer winnaarId = (wedstrijdResult.getObject("winnaar") != null) ? wedstrijdResult.getInt("winnaar") : null;
            String score = wedstrijdResult.getString("score");
            int finale = wedstrijdResult.getInt("finale");

            // Maak een Wedstrijd object aan en voeg het toe aan de speler
            Wedstrijd wedstrijd = new Wedstrijd(wedstrijdId, tornooiId, speler1Id, speler2Id, winnaarId, score, finale);
            found_speler.addWedstrijd(wedstrijd);
        }

        // Sluit de ResultSets en Statement-objecten af
        spelerResult.close();
        tornooiResult.close();
        wedstrijdResult.close();
        spelerStmt.close();
        tornooiStmt.close();
        wedstrijdStmt.close();

        // Commit de veranderingen (als er iets in de DB is aangepast)
        connection.commit();

    } catch (Exception e) {
        throw new InvalidSpelerException(String.valueOf(tennisvlaanderenId));    
    }

    return found_speler;
}

  
  
  @Override
  public List<Speler> getAllSpelers() {
    //DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeert zodat de testen slagen.
      ArrayList<Speler> spelers = new ArrayList<Speler>();

      try {
        // Haal alle spelers op
        String spelerQuery = "SELECT * FROM speler";
        PreparedStatement spelerStmt = connection.prepareStatement(spelerQuery);
        ResultSet spelerResult = spelerStmt.executeQuery();

        while (spelerResult.next()) {
            int id = spelerResult.getInt("tennisvlaanderenid");
            int punten = spelerResult.getInt("punten");
            String naam = spelerResult.getString("naam");

            // Maak de Speler object aan
            Speler speler = new Speler(id, naam, punten);

            // Haal de tornooien op voor deze speler
            String tornooiQuery = "SELECT * FROM speler_speelt_tornooi WHERE speler = ?";
            PreparedStatement tornooiStmt = connection.prepareStatement(tornooiQuery);
            tornooiStmt.setInt(1, id);
            ResultSet tornooiResult = tornooiStmt.executeQuery();

            while (tornooiResult.next()) {
                int tornooiId = tornooiResult.getInt("tornooi");

                // Haal de tornooi gegevens op binnen de loop
                String tornooiDetailsQuery = "SELECT * FROM tornooi WHERE id = ?";
                PreparedStatement tornooiDetailsStmt = connection.prepareStatement(tornooiDetailsQuery);
                tornooiDetailsStmt.setInt(1, tornooiId);
                ResultSet tornooiDetailsResult = tornooiDetailsStmt.executeQuery();

                if (tornooiDetailsResult.next()) {
                    String clubnaam = tornooiDetailsResult.getString("clubnaam");

                    // Maak een Tornooi object aan en voeg het toe aan de speler
                    Tornooi tornooi = new Tornooi(tornooiId, clubnaam);
                    speler.addTornooi(tornooi);
                }

                tornooiDetailsResult.close();
                tornooiDetailsStmt.close();
            }

            // Haal de wedstrijden op voor deze speler
            String wedstrijdQuery = "SELECT * FROM wedstrijd WHERE speler1 = ? OR speler2 = ?";
            PreparedStatement wedstrijdStmt = connection.prepareStatement(wedstrijdQuery);
            wedstrijdStmt.setInt(1, id);
            wedstrijdStmt.setInt(2, id);
            ResultSet wedstrijdResult = wedstrijdStmt.executeQuery();

            while (wedstrijdResult.next()) {
                int wedstrijdId = wedstrijdResult.getInt("id");
                int tornooiId = wedstrijdResult.getInt("tornooi");
                int speler1Id = wedstrijdResult.getInt("speler1");
                int speler2Id = wedstrijdResult.getInt("speler2");
                Integer winnaarId = (wedstrijdResult.getObject("winnaar") != null) ? wedstrijdResult.getInt("winnaar") : null;
                String score = wedstrijdResult.getString("score");
                int finale = wedstrijdResult.getInt("finale");

                // Maak een Wedstrijd object aan en voeg het toe aan de speler
                Wedstrijd wedstrijd = new Wedstrijd(wedstrijdId, tornooiId, speler1Id, speler2Id, winnaarId, score, finale);
                speler.addWedstrijd(wedstrijd);
            }

            // Voeg de speler toe aan de lijst
            spelers.add(speler);

            // Sluit de ResultSets en Statements
            tornooiResult.close();
            wedstrijdResult.close();
            tornooiStmt.close();
            wedstrijdStmt.close();
        }

        // Sluit de spelerResult en spelerStmt
        spelerResult.close();
        spelerStmt.close();

        // Commit de veranderingen
        connection.commit();

    } catch (Exception e) {
        throw new InvalidSpelerException(String.valueOf(e));    
    }

    return spelers;
}

  

@Override
public void updateSpelerInDb(Speler speler) {
    try {
        // Controleer eerst of de speler bestaat
        String checkSpelerQuery = "SELECT COUNT(*) AS count FROM speler WHERE tennisvlaanderenid = ?";
        PreparedStatement checkStmt = connection.prepareStatement(checkSpelerQuery);
        checkStmt.setInt(1, speler.getTennisvlaanderenId());
        ResultSet rs = checkStmt.executeQuery();
        rs.next();
        int count = rs.getInt("count");
        rs.close();
        checkStmt.close();

        if (count == 0) {
            throw new InvalidSpelerException(String.valueOf(speler.getTennisvlaanderenId()));
        }

        // Update de naam en punten van de speler
        String updateSpelerQuery = "UPDATE speler SET naam = ?, punten = ? WHERE tennisvlaanderenid = ?";
        PreparedStatement updateSpelerStmt = connection.prepareStatement(updateSpelerQuery);
        updateSpelerStmt.setString(1, speler.getNaam());
        updateSpelerStmt.setInt(2, speler.getPunten());
        updateSpelerStmt.setInt(3, speler.getTennisvlaanderenId());
        updateSpelerStmt.executeUpdate();
        updateSpelerStmt.close();

        // Verwijder bestaande tornooi-relaties
        String deleteTornooienQuery = "DELETE FROM speler_speelt_tornooi WHERE speler = ?";
        PreparedStatement deleteTornooienStmt = connection.prepareStatement(deleteTornooienQuery);
        deleteTornooienStmt.setInt(1, speler.getTennisvlaanderenId());
        deleteTornooienStmt.executeUpdate();
        deleteTornooienStmt.close();

        // Voeg nieuwe tornooi-relaties toe
        String insertTornooiQuery = "INSERT INTO speler_speelt_tornooi (speler, tornooi) VALUES (?, ?)";
        PreparedStatement insertTornooiStmt = connection.prepareStatement(insertTornooiQuery);
        for (Tornooi t : speler.getTornooien()) {
            insertTornooiStmt.setInt(1, speler.getTennisvlaanderenId());
            insertTornooiStmt.setInt(2, t.getId());
            insertTornooiStmt.addBatch();
        }
        insertTornooiStmt.executeBatch();
        insertTornooiStmt.close();

        connection.commit();

    } catch (InvalidSpelerException e) {
        // Laat deze doorgaan
        throw e;
    } catch (Exception e) {
        throw new RuntimeException("Fout bij het updaten van speler met ID " + speler.getTennisvlaanderenId(), e);
    }
}



@Override
public void deleteSpelerInDb(int tennisvlaanderenid) {
    try {
        // Controleer of de speler bestaat
        PreparedStatement checkSpeler = connection.prepareStatement(
            "SELECT COUNT(*) FROM speler WHERE tennisvlaanderenid = ?");
        checkSpeler.setInt(1, tennisvlaanderenid);
        ResultSet rs = checkSpeler.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        checkSpeler.close();

        if (count == 0) {
            throw new InvalidSpelerException(String.valueOf(tennisvlaanderenid));
        }

        // Verwijder speler uit speler_speelt_tornooi
        PreparedStatement deleteTornooiRelaties = connection.prepareStatement(
            "DELETE FROM speler_speelt_tornooi WHERE speler = ?");
        deleteTornooiRelaties.setInt(1, tennisvlaanderenid);
        deleteTornooiRelaties.executeUpdate();
        deleteTornooiRelaties.close();

        // Verwijder alle wedstrijden waarin de speler voorkomt
        PreparedStatement deleteWedstrijden = connection.prepareStatement(
            "DELETE FROM wedstrijd WHERE speler1 = ? OR speler2 = ?");
        deleteWedstrijden.setInt(1, tennisvlaanderenid);
        deleteWedstrijden.setInt(2, tennisvlaanderenid);
        deleteWedstrijden.executeUpdate();
        deleteWedstrijden.close();

        // Verwijder speler uit speler tabel
        PreparedStatement deleteSpeler = connection.prepareStatement(
            "DELETE FROM speler WHERE tennisvlaanderenid = ?");
        deleteSpeler.setInt(1, tennisvlaanderenid);
        deleteSpeler.executeUpdate();
        deleteSpeler.close();

        connection.commit();
    } catch (InvalidSpelerException e) {
        throw e; // Hergooi specifieke exception
    } catch (Exception e) {
        throw new RuntimeException("Fout bij verwijderen van speler", e);
    }
}


@Override
public String getHoogsteRankingVanSpeler(int tennisvlaanderenid) {
    String besteTornooi = null; // Declare outside the try block
    String besteFase = null;    // Declare outside the try block

    try {
        String query = "SELECT w.finale, w.winnaar, t.clubnaam " +
                       "FROM wedstrijd w JOIN tornooi t ON w.tornooi = t.id " +
                       "WHERE w.speler1 = ? OR w.speler2 = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setInt(1, tennisvlaanderenid);
        stmt.setInt(2, tennisvlaanderenid);

        ResultSet rs = stmt.executeQuery();

        int hoogsteScore = -1;

        while (rs.next()) {
            int finale = rs.getInt("finale");
            int winnaar = rs.getInt("winnaar");
            String clubnaam = rs.getString("clubnaam");

            int score = switch (finale) {
                case 1 -> (winnaar == tennisvlaanderenid) ? 3 : 2;
                case 2 -> 1;
                default -> 0;
            };

            if (score > hoogsteScore) {
                hoogsteScore = score;
                besteTornooi = clubnaam;
                besteFase = switch (score) {
                    case 3 -> "winst";
                    case 2 -> "finale";
                    case 1 -> "halve finale";
                    default -> null;
                };
            }
        }

        if (besteTornooi == null || besteFase == null) {
            throw new InvalidSpelerException(String.valueOf(tennisvlaanderenid));
        }

        rs.close();
        stmt.close();

    } catch (SQLException e) {
        throw new InvalidSpelerException(String.valueOf(tennisvlaanderenid));
    }

    String resultaat = "Hoogst geplaatst in het tornooi van " + besteTornooi + " met plaats in de " + besteFase;
    return resultaat;
}







@Override
public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId) {
    //DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeert zodat de testen slagen.

    try {
        PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO speler_speelt_tornooi (speler, tornooi) VALUES (?, ?)");
        stmt.setInt(1, tennisvlaanderenId);
        stmt.setInt(2, tornooiId);
        stmt.executeUpdate();
        stmt.close();
        connection.commit();
      } catch (Exception e) {
        throw new InvalidSpelerException(String.valueOf(tennisvlaanderenId));    
     }
}

@Override
public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
    //DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeert zodat de testen slagen.

    try {
        PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM speler_speelt_tornooi WHERE speler = ? AND tornooi = ?");
        stmt.setInt(1, tennisvlaanderenId);
        stmt.setInt(2, tornooiId);
        stmt.executeUpdate();
        stmt.close();
        connection.commit();
      } catch (Exception e) {
        throw new InvalidSpelerException(String.valueOf(tennisvlaanderenId));    
    }
  }
}