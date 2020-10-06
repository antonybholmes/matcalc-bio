package edu.columbia.rdf.matcalc.bio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.genomic.Sequence;

/**
 * We can either search genomic DNA where the location is useful, or else random
 * DNA sequences.
 * 
 * @author antony
 *
 */
public class SearchSequence implements Comparable<SearchSequence> {
  private String mId;
  private Sequence mDna;
  private SearchSequenceType mType;
  private GenomicRegion mRegion;

  public SearchSequence(String id, Sequence dna) {
    mId = id;
    mDna = dna;
    mType = SearchSequenceType.DNA;
  }

  public SearchSequence(GenomicRegion region, Sequence dna) {
    mRegion = region;
    mId = region.getLocation();
    mDna = dna;
    mType = SearchSequenceType.GENOMIC;
  }

  public String getId() {
    return mId;
  }

  public Sequence getDna() {
    return mDna;
  }

  public SearchSequenceType getType() {
    return mType;
  }

  public GenomicRegion getRegion() {
    return mRegion;
  }

  public static List<SearchSequence> reverseComplement(Collection<SearchSequence> sequences) {
    List<SearchSequence> ret = new ArrayList<SearchSequence>(sequences.size());

    for (SearchSequence s : sequences) {
      Sequence rcDna = Sequence.reverseComplement(s.mDna);

      switch (s.mType) {
      case GENOMIC:
        ret.add(new SearchSequence(s.mRegion, rcDna));
        break;
      default:
        ret.add(new SearchSequence(s.mId, rcDna));
        break;
      }
    }

    return ret;
  }

  public static <X extends SearchSequence> byte[][] toIndex(List<X> seqs) {
    byte[][] ret = new byte[seqs.size()][];

    for (int i = 0; i < seqs.size(); ++i) {
      ret[i] = seqs.get(i).getDna().toIndex();
    }

    return ret;
  }

  @Override
  public int compareTo(SearchSequence s) {
    if (mRegion != null && s.mRegion != null) {
      return mRegion.compareTo(s.mRegion);
    } else {
      return mDna.compareTo(s.mDna);
    }
  }

}
