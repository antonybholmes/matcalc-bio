package edu.columbia.rdf.matcalc.bio;

import org.jebtk.bioinformatics.genomic.Genome;

public class GenomeDatabase {
  private Genome mGenome;
  private String mDb;

  public GenomeDatabase(Genome genome, String db) {
    mGenome = genome;
    mDb = db;
  }

  public Genome getGenome() {
    return mGenome;
  }
  
  public String getDb() {
    return mDb;
  }
  
  @Override
  public String toString() {
    return mGenome.getAssembly();
  }
}
