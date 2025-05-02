package be.kuleuven;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.List;


import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class SpelerRepositoryJPAimpl implements SpelerRepository {
  private final EntityManager em;
  public static final String PERSISTANCE_UNIT_NAME = "be.kuleuven.spelerhibernateTest";

  // Constructor
  SpelerRepositoryJPAimpl(EntityManager entityManager) {
    // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
    this.em = entityManager;
  }

  @Override
  public void addSpelerToDb(Speler speler) {
      // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
      EntityTransaction tx = em.getTransaction();
      try {
          tx.begin();  // Begin een nieuwe transactie
          em.persist(speler);  // Voeg de speler toe aan de database
          tx.commit();  // Commit de transactie, waardoor de speler daadwerkelijk wordt opgeslagen in de database
      } catch (Exception e) {
          if (tx.isActive()) tx.rollback();  // Rollback de transactie als er een fout optreedt
          throw e;  // Gooi de uitzondering verder
      }
  }
  

  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
      // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
      Speler speler = em.find(Speler.class, tennisvlaanderenId);
      if (speler == null) {
          throw new InvalidSpelerException("Invalid Speler met identification: " + tennisvlaanderenId);
      }
      return speler;
  }
  
  @Override
  public List<Speler> getAllSpelers() {
      // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Speler> cq = cb.createQuery(Speler.class);
      Root<Speler> rootEntry = cq.from(Speler.class);
      cq.select(rootEntry);
      return em.createQuery(cq).getResultList();  // Haal alle spelers op uit de database
  }
  

  @Override
  public void updateSpelerInDb(Speler speler) {
      // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
      EntityTransaction tx = em.getTransaction();
      try {
          tx.begin();  // Begin een nieuwe transactie
          if (em.find(Speler.class, speler.getTennisvlaanderenId()) == null) {
              throw new InvalidSpelerException("Invalid Speler met identification: " + speler.getTennisvlaanderenId());
          }
          em.merge(speler);  // Update de speler in de database
          tx.commit();  // Commit de transactie
      } catch (Exception e) {
          if (tx.isActive()) tx.rollback();  // Rollback als er een fout optreedt
          throw e;  // Gooi de fout verder
      }
  }
  

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenId) {
      // DONE: verwijder de "throw new UnsupportedOperationException" en schrijf de code die de gewenste methode op de juiste manier implementeerd zodat de testen slagen.
      EntityTransaction tx = em.getTransaction();
      try {
          tx.begin();
          Speler speler = em.find(Speler.class, tennisvlaanderenId);
          if (speler == null) {
              throw new InvalidSpelerException("Invalid Speler met identification: " + tennisvlaanderenId);
          }
          em.remove(speler);  // Verwijder de speler uit de database
          tx.commit();
      } catch (Exception e) {
          if (tx.isActive()) tx.rollback();  // Rollback als er iets misgaat
          throw e;  // Gooi de fout verder
      }
  }
  
  
  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenId) {
      String query = "SELECT w.finale, w.winnaar, t.clubnaam " +
                     "FROM wedstrijd w " +
                     "JOIN tornooi t ON w.tornooi = t.id " +
                     "WHERE w.speler1 = :id OR w.speler2 = :id";
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> results = (List<Map<String, Object>>) em.createQuery(query)
        .setParameter("id", tennisvlaanderenId)
        .getResultList();
  
      String highestRanking = "Geen ranking beschikbaar";
      for (Map<String, Object> result : results) {
        String finale = (String) result.get("finale");
        Boolean winner = (Boolean) result.get("winnaar");
        String clubnaam = (String) result.get("clubnaam");
  
        if (winner) {
          highestRanking = "Winnaar in toernooi: " + clubnaam;
        } else if ("finale".equals(finale)) {
          highestRanking = "Finale in toernooi: " + clubnaam;
        }
      }
      return highestRanking;
  }
  
  


  @Override
  public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId) {
      EntityTransaction tx = em.getTransaction();
      try {
          tx.begin();
          Speler speler = em.find(Speler.class, tennisvlaanderenId);
          Tornooi tornooi = em.find(Tornooi.class, tornooiId);
          if (speler == null || tornooi == null) {
              throw new IllegalArgumentException("Speler of Tornooi niet gevonden");
          }
          speler.getTornooien().add(tornooi);  // Voeg het toernooi toe aan de speler
          em.merge(speler);  // Sla de speler op met het toegevoegde toernooi
          tx.commit();
      } catch (Exception e) {
          if (tx.isActive()) tx.rollback();
          throw e;
      }
  }
  

  @Override
  public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
    EntityTransaction tx = em.getTransaction();

    try {
      tx.begin();

      Speler speler = em.find(Speler.class, tennisvlaanderenId);
      Tornooi tornooi = em.find(Tornooi.class, tornooiId);

      if (speler == null || tornooi == null) {
        throw new IllegalArgumentException("Speler or Tornooi not found");
      }

      speler.getTornooien().remove(tornooi);
      em.merge(speler);

      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }
}