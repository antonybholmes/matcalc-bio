/**
 * Copyright 2016 Antony Holmes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.columbia.rdf.matcalc.bio.toolbox.dna;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jebtk.bioinformatics.Fasta;
import org.jebtk.bioinformatics.dna.Sequence;
import org.jebtk.bioinformatics.ui.filters.FastaGuiFileFilter;
import org.jebtk.bioinformatics.ui.filters.FastaSaveGuiFileFilter;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.modern.io.GuiFileExtFilter;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;


/**
 * Allow users to open and save Broad GCT files
 *
 * @author Antony Holmes Holmes
 *
 */
public class FastaIOModule extends CalcModule  {
	private static final GuiFileExtFilter OPEN_FILTER = 
			new FastaGuiFileFilter();

	private static final GuiFileExtFilter SAVE_FILTER = 
			new FastaSaveGuiFileFilter();

	public FastaIOModule() {
		registerFileOpenType(OPEN_FILTER);
		registerFileSaveType(SAVE_FILTER);
	}
	
	/* (non-Javadoc)
	 * @see org.abh.lib.NameProperty#getName()
	 */
	@Override
	public String getName() {
		return "FASTA IO";
	}
		
	@Override
	public AnnotationMatrix autoOpenFile(final MainMatCalcWindow window,
			final Path file,
			boolean hasHeader,
			List<String> skipMatches,
			int rowAnnotations,
			String delimiter) throws IOException {
		return toMatrix(file);
	}		

	@Override
	public boolean saveFile(final MainMatCalcWindow window,
			final Path file, 
			final AnnotationMatrix m) throws IOException {
		
		List<Sequence> sequences = toSequences(window, m);
		
		if (sequences.size() == 0) {
			return false;
		}
		
		Fasta.write(file, sequences);
		
		return true;
	}
	
	public static List<Sequence> toSequences(final MainMatCalcWindow window,
			final AnnotationMatrix m) {
		
		int c1 = AnnotationMatrix.findColumn(m, "Name");
		
		if (c1 == -1) {
			c1 = AnnotationMatrix.findColumn(m, "DNA Location");
		}
		
		if (c1 == -1) {
			c1 = AnnotationMatrix.findColumn(m, "Location");
		}
		
		int c2 = AnnotationMatrix.findColumn(m, "DNA Sequence");
		
		if (c2 == -2) {
			return Collections.emptyList();
		}
		
		List<Sequence> sequences = new ArrayList<Sequence>(m.getRowCount());
		
		for (int i = 0; i < m.getRowCount(); ++i) {
			String name = c1 != -1 ? m.getText(i, c1) : "Sequence " + (i + 1);
			
			sequences.add(Sequence.create(name, m.getText(i, c2)));
		}

		return sequences;
	}
	
	public static AnnotationMatrix toMatrix(Path file) throws IOException {
		return toMatrix(Fasta.parse(file));
	}
	
	public static AnnotationMatrix toMatrix(List<Sequence> sequences) {
		
		AnnotationMatrix ret = AnnotatableMatrix
				.createAnnotatableMixedMatrix(sequences.size(), 2);
		
		ret.setColumnName(0, "Name");
		ret.setColumnName(1, "DNA Sequence");
		
		System.err.println("seqences " + sequences.size());
		
		for (int i = 0; i < sequences.size(); ++i) {
			Sequence s = sequences.get(i);
			
			ret.set(i, 0, s.getName());
			ret.set(i, 1, s.toString());
		}
		
		return ret;
	}
}
