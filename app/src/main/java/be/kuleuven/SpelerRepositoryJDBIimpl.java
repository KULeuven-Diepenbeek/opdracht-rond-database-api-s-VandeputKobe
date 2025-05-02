package be.kuleuven;

import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import javax.naming.OperationNotSupportedException;

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
        "INSERT INTO speler (tennisvlaanderenid, naam, ranking) VALUES (?, ?, ?)",
        speler.getTennisvlaanderenId(), speler.getNaam(), speler.getPunten()
      );
    });
  }


  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    return (Speler) jdbi.withHandle(handle ->
      handle.createQuery("SELECT * FROM speler WHERE tennisvlaanderenid = :id")
            .bind("id", tennisvlaanderenId)
            .mapToBean(Speler.class)
            .findOne()
            .orElse(null)
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
      return handle.createUpdate("UPDATE speler SET naam = :naam, ranking = :ranking WHERE tennisvlaanderenid = :id")
        .bindBean(speler)
        .execute();
    });
    if (affectedRows == 0) {
      throw new InvalidSpelerException("Speler met tennisvlaanderenid " + speler.getTennisvlaanderenId() + " niet gevonden.");
    }
  }

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenid) {
    // TODO: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    int affectedRows = jdbi.withHandle(handle -> {
      return handle.createUpdate("DELETE FROM speler WHERE tennisvlaanderenid = :id")
        .bind("id", tennisvlaanderenid)
        .execute();
    });
    if (affectedRows == 0) {
      throw new InvalidSpelerException("Speler met tennisvlaanderenid " + tennisvlaanderenid + " niet gevonden.");
    }
  }

  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenid) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    return jdbi.withHandle(handle -> 
      handle.createQuery("""
        SELECT resultaat FROM deelname
        WHERE speler_id = :id
        ORDER BY 
          CASE resultaat
            WHEN 'Winst' THEN 1
            WHEN 'Finale' THEN 2
            WHEN 'Halve finale' THEN 3
            WHEN 'Kwartfinale' THEN 4
            ELSE 5
          END
        LIMIT 1
      """)
      .bind("id", tennisvlaanderenid)
      .mapTo(String.class)
      .findOne()
      .orElse(null)
    );
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
