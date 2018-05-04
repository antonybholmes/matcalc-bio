package edu.columbia.rdf.matcalc.bio;

public class GenomeDatabase {
  private String mGenome;
  private String mDb;

  public GenomeDatabase(String genome, String db) {
    mGenome = genome;
    mDb = db;
  }

  public String getGenome() {
    return mGenome;
  }
  
  public String getDb() {
    return mDb;
  }
  
  @Override
  public String toString() {
    return mGenome;
  }
}
