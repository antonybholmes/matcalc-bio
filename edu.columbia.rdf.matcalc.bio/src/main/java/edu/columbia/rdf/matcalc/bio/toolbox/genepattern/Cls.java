/**
 * Copyright (C) 2016, Antony Holmes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of copyright holder nor the names of its contributors 
 *     may be used to endorse or promote products derived from this software 
 *     without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.columbia.rdf.matcalc.bio.toolbox.genepattern;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jebtk.core.collections.UniqueArrayList;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.text.TextUtils;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.math.matrix.DataFrame;

/**
 * The class Cls.
 */
public class Cls {

  /**
   * The constant UNDEF_GROUP.
   */
  public static final String UNDEF_GROUP = "na";

  /**
   * Write.
   *
   * @param file    the file
   * @param mGroups the m groups
   * @param matrix  the matrix
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void write(Path file, List<XYSeries> mGroups, DataFrame matrix) throws IOException {
    // First allocate everyone to the undefined group

    Map<String, String> groupMap = new HashMap<String, String>();

    for (String name : matrix.getColumnNames()) {
      groupMap.put(name, UNDEF_GROUP);
    }

    for (XYSeries series : mGroups) {
      List<Integer> indices = XYSeries.findColumnIndices(matrix, series);

      for (int i : indices) {
        groupMap.put(matrix.getColumnName(i), series.getName());
      }
    }

    // Now make a list of the unique group names in the order they appear

    List<String> names = new UniqueArrayList<String>();

    for (String name : matrix.getColumnNames()) {
      names.add(groupMap.get(name));
    }

    write(file, names, groupMap, matrix);
  }

  public static void write(Path file, List<String> names, Map<String, String> groupMap, DataFrame matrix)
      throws IOException {
    BufferedWriter writer = FileUtils.newBufferedWriter(file);

    try {
      writer.write(Integer.toString(matrix.getCols()));
      writer.write(TextUtils.SPACE_DELIMITER);
      writer.write(Integer.toString(names.size()));
      writer.write(" 1");
      writer.newLine();

      writer.write("#");

      for (String name : names) {
        writer.write(TextUtils.SPACE_DELIMITER);
        writer.write(name);
      }

      writer.newLine();

      for (int i = 0; i < matrix.getCols(); ++i) {
        writer.write(groupMap.get(matrix.getColumnName(i)));

        if (i < matrix.getCols() - 1) {
          writer.write(TextUtils.SPACE_DELIMITER);
        }
      }

      writer.newLine();
    } finally {
      writer.close();
    }

  }
}
