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
package edu.columbia.rdf.matcalc.bio.toolbox.external.ucsc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.jebtk.bioinformatics.ext.ucsc.Bed;
import org.jebtk.bioinformatics.genomic.Chromosome;
import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.ui.external.ucsc.BedGuiFileFilter;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.Io;
import org.jebtk.core.text.Join;
import org.jebtk.core.text.TextUtils;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.modern.io.GuiFileExtFilter;

import edu.columbia.rdf.matcalc.FileType;
import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;


/**
 * Allow users to open and save Broad GCT files
 *
 * @author Antony Holmes Holmes
 *
 */
public class BedIOModule extends CalcModule  {
	private static final GuiFileExtFilter FILTER = new BedGuiFileFilter();


	public BedIOModule() {
		registerFileOpenType(FILTER);
		registerFileSaveType(FILTER);
	}
	
	/* (non-Javadoc)
	 * @see org.abh.lib.NameProperty#getName()
	 */
	@Override
	public String getName() {
		return "BED IO";
	}
		
	@Override
	public AnnotationMatrix autoOpenFile(final MainMatCalcWindow window,
			final Path file,
			FileType type,
			int headers,
			int rowAnnotations,
			String delimiter,
			Collection<String> skipLines) throws IOException {
		return Bed.toMatrix(file);
	}		

	@Override
	public boolean saveFile(final MainMatCalcWindow window,
			final Path file, 
			final AnnotationMatrix m) throws IOException {
		BufferedWriter writer = FileUtils.newBufferedWriter(file);

		try {
			writer.write("track name=\"Locations\" description=\"Locations\"");
			writer.newLine();

			for (int i = 0; i < m.getRowCount(); ++i) {
				GenomicRegion r = getRegion(m, i);
				
				if (r != null) {
					writer.write(Join.onTab().values(r.getChr(), r.getStart(), r.getEnd(), r.getLocation()).toString());
				
					writer.newLine();
				}
				
				/*
				String s = m.getText(i, 0) + ":" + m.getText(i, 1) + "-" + m.getText(i, 2);
				
				writer.write(Join.onTab().values(m.getText(i, 0), 
						m.getText(i, 1),
						m.getText(i, 2),
						s).toString());
				*/
				
				
			}
		} finally {
			writer.close();
		}
		
		return true;
	}
	
	public static GenomicRegion getRegion(final AnnotationMatrix m, int row) {
		GenomicRegion region = null;

		if (Io.isEmptyLine(m.getText(row, 0))) {
			region = null;
		} else if (m.getText(row, 0).contains(TextUtils.NA)) {
			region = null;
		} else if (GenomicRegion.isGenomicRegion(m.getText(row, 0))) {
			region = GenomicRegion.parse(m.getText(row, 0));
		} else if (isThreeColumnGenomicLocation(m, row)) {
			// three column format

			region = new GenomicRegion(Chromosome.parse(m.getText(row, 0)),
					Integer.parseInt(m.getText(row, 1)),
					Integer.parseInt(m.getText(row, 2)));
		} else {
			region = null;
		}
		
		return region;
	}
	
	public static boolean isThreeColumnGenomicLocation(AnnotationMatrix m, int row) {
		if (!GenomicRegion.isChr(m.getText(row, 0))) {
			return false;
		}

		if (!TextUtils.isNumber(m.getText(row, 1))) {
			return false;
		}

		if (!TextUtils.isNumber(m.getText(row, 2))) {
			return false;
		}

		return true;
	}
}
