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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.jebtk.core.ColorUtils;
import org.jebtk.core.Mathematics;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.Io;
import org.jebtk.core.io.PathUtils;
import org.jebtk.core.text.Formatter;
import org.jebtk.core.text.TextUtils;
import org.jebtk.graphplot.PlotFactory;
import org.jebtk.graphplot.figure.Axes;
import org.jebtk.graphplot.figure.Axes;
import org.jebtk.graphplot.figure.Axis;
import org.jebtk.graphplot.figure.Figure;
import org.jebtk.graphplot.figure.LabelPlotLayer;
import org.jebtk.graphplot.figure.Plot;
import org.jebtk.graphplot.figure.RightLabelPlotLayer;
import org.jebtk.graphplot.figure.SubFigure;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.plotbox.PlotBoxRowLayout;
import org.jebtk.math.Linspace;
import org.jebtk.math.matrix.AnnotatableMatrix;
import org.jebtk.math.matrix.AnnotationMatrix;
import org.jebtk.math.matrix.Matrix;
import org.jebtk.modern.UIService;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.graphics.colormap.ColorMap;
import org.jebtk.modern.io.FileDialog;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.ribbon.RibbonLargeButton;
import org.xml.sax.SAXException;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.figure.graph2d.Graph2dWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;

/**
 * The class BoxWhiskerPlotModule.
 */
public class GseaPlotModule extends CalcModule implements ModernClickListener {

	private static final Color RED_COLOR = Color.RED;
	
	private static final Color RED_COLOR_2 = 
			ColorUtils.saturation(RED_COLOR, 0.1);
	
	private static final Color BLUE_COLOR =  
			ColorUtils.decodeHtmlColor("#3771c8");
	
	private static final Color BLUE_COLOR_2 = 
			ColorUtils.saturation(BLUE_COLOR, 0.1);
	
	private static final Color GREEN_COLOR =  
			ColorUtils.decodeHtmlColor("#00aa44");

	private static final int DEFAULT_WIDTH = 1000;
	
	/**
	 * The member parent.
	 */
	private MainMatCalcWindow mParent;

	/* (non-Javadoc)
	 * @see org.abh.lib.NameProperty#getName()
	 */
	@Override
	public String getName() {
		return "GSEA Plot";
	}

	/* (non-Javadoc)
	 * @see edu.columbia.rdf.apps.matcalc.modules.Module#init(edu.columbia.rdf.apps.matcalc.MainMatCalcWindow)
	 */
	@Override
	public void init(MainMatCalcWindow window) {
		mParent = window;

		RibbonLargeButton button = new RibbonLargeButton("GSEA Plot", 
				UIService.getInstance().loadIcon("line_graph", 24),
				"GSEA Plot",
				"Create a GSEA Plot.");
		button.addClickListener(this);

		mParent.getRibbon().getToolbar("Bioinformatics").getSection("GSEA").add(button);
	}

	/* (non-Javadoc)
	 * @see org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.modern.event.ModernClickEvent)
	 */
	@Override
	public void clicked(ModernClickEvent e) {
		try {
			plot();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Export.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ParseException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	private void plot() throws IOException, ParseException, SAXException, ParserConfigurationException {
		Path dir = FileDialog.open(mParent).dirs().getFile(RecentFilesService.getInstance().getPwd());
		
		if (dir == null) {
			return;
		}
		
		System.err.println(dir);
		
		Path rankedListFile = FileUtils.find(dir, "ranked_gene_list");
		
		if (rankedListFile == null) {
			return;
		}
		
		System.err.println(rankedListFile);
		
		String p1 = PathUtils.getName(rankedListFile);
		p1 = p1.replace("ranked_gene_list_", "");
		p1 = p1.replaceFirst("_versus.+", "");
		
		String p2 = PathUtils.getName(rankedListFile);
		p2 = p2.replaceFirst(".+versus_", "");
		p2 = p2.replaceFirst("_[^_]+\\.xls", "");
		
		System.err.println(rankedListFile);
		
		//
		// Get some stats from each report
		//
		
		Map<String, Integer> sizeMap = new HashMap<String, Integer>();
		Map<String, Double> esMap = new HashMap<String, Double>();
		Map<String, Double> nesMap = new HashMap<String, Double>();
		Map<String, Double> pMap = new HashMap<String, Double>();
		Map<String, Double> fdrMap = new HashMap<String, Double>();
		
		List<Path> reportFiles = FileUtils.findMatch(dir, "gsea_report", "xls");
		
		String line;
		List<String> tokens;
		
		for (Path file : reportFiles) {
			BufferedReader reader = FileUtils.newBufferedReader(file);
			
			System.err.println("report file " + file);
			
			try {
				reader.readLine();
				
				while ((line = reader.readLine()) != null) {
					tokens = TextUtils.tabSplit(line);
					
					String name = tokens.get(0);
					
					if (tokens.get(3).equals(TextUtils.NULL)) {
						continue;
					}
					
					System.err.println("n " + name);
					
					int size = TextUtils.parseInt(tokens.get(3));
					double es = TextUtils.parseDouble(tokens.get(4));
					double nes = TextUtils.parseDouble(tokens.get(5));
					double p = TextUtils.parseDouble(tokens.get(6));
					double fdr = TextUtils.parseDouble(tokens.get(7));
					
					sizeMap.put(name, size);
					esMap.put(name, es);
					nesMap.put(name, nes);
					pMap.put(name, p);
					fdrMap.put(name, fdr);
				}
			} finally {
				reader.close();
			}
		}
		
		//
		// Find where the crossing point is in the full list of ranked genes
		//
		
		AnnotationMatrix allRankedGenes = 
				AnnotatableMatrix.copyInnerColumns(AnnotationMatrix.parseTxtMatrix(rankedListFile, 
						true,
						TextUtils.emptyList(),
						0,
						TextUtils.TAB_DELIMITER), 
						4, 
						4);
		
		// Make the first column  the rank order
		
		allRankedGenes.setColumnName(0, "Ranked x");
		allRankedGenes.setColumnName(1, "Ranked y");
		
		
		Matrix.setColumn(0, Linspace.evenlySpaced(1, allRankedGenes.getRowCount()), allRankedGenes);
		
		System.err.println("aha " + allRankedGenes.getValue(allRankedGenes.getRowCount() - 1, 0) + " " + allRankedGenes.getValue(allRankedGenes.getRowCount() - 1, 1));
		
		
		int crossingIndex = -1;
		
		for (int i = 0; i < allRankedGenes.getRowCount(); ++i) {
			double v = allRankedGenes.getValue(i, 1);
			
			if (Mathematics.isInvalidNumber(v)) {
				continue;
			}
			
			if (v < 0) {
				// zero index based rank
				crossingIndex = i - 1;
				break;
			}
		}
		
		Color color;
		
		
		//
		// Go through all comparison files and create a plot for each
		//

		List<Path> plotFiles = getPlotFiles(dir);
		
		for (Path plotFile : plotFiles) {
			if (!FileUtils.exists(plotFile)) {
				continue;
			}
			
			String name = PathUtils.getNameNoExt(plotFile);
			
			System.err.println("plot " + plotFile);
			System.err.println("name " + name);
			
			if (!esMap.containsKey(name))  {
				continue;
			}
			
			double es = esMap.get(name);
			
			Figure figure = new Figure("GSEA Figure", new PlotBoxRowLayout());
			
			SubFigure subFigure = figure.newSubFigure();
			
			Axes axes = subFigure.newAxes();
			
			// Select the xy columns of interest
			
			AnnotationMatrix m = AnnotatableMatrix.copyInnerColumns(AnnotationMatrix.parseTxtMatrix(plotFile, 
							true,
							TextUtils.emptyList(),
							0,
							TextUtils.TAB_DELIMITER), 
							5, 
							7, 
							8);
			
			
			// split into up and down
			
			// Define the zero point as half way between the last positive
			// and first negative point
			
			int geneSetCrossingIndex = -1;
			double crossingX = -1;
			
			for (int i = 0; i < m.getRowCount(); ++i) {
				if (m.getValue(i, 1) < 0) {
					geneSetCrossingIndex = i;
					
					//crossingX = (m.getValue(i, 0) + m.getValue(i - 1, 0)) / 2.0;
					
					// Imagine p1 is (0,0) so we have y =mx and solve for x
					double dydx = (m.getValue(i, 1) - m.getValue(i - 1, 1)) / (m.getValue(i, 0) - m.getValue(i - 1, 0));
					
					// Since we set p1 (i - 1) to be the zero point, when we
					// solve for the intercept at y = 0, invert p1 as that
					// is the distance of y = 0 relative to p1
					
					crossingX = m.getValue(i - 1, 0) - m.getValue(i - 1, 1) / dydx;
					
					break;
				}
			}
			
			System.err.println("cross " + geneSetCrossingIndex + " " + crossingX + " " + m.getValue(geneSetCrossingIndex, 1));
			
			
			// 
			// Ranking
			//
			
			AnnotationMatrix rankedM = 
					AnnotatableMatrix.createNumericalMatrix(m.getRowCount(), 3);
			
			rankedM.setColumnName(0, "Ranked x");
			rankedM.setColumnName(1, "Ranked y");
			rankedM.setColumnName(1, "Ranked z");
			rankedM.copyColumn(m, 0, 0);
			
			//AnnotationMatrix.setColumn(1, 1, ranked);
			AnnotationMatrix.setColumn(1, 0, rankedM);
			
			// Create the z
			rankedM.copyColumn(m, 0, 2);
			
			for (int i = 0; i < rankedM.getRowCount(); ++i) {
				rankedM.set(i, 
						2, 
						(crossingIndex - rankedM.getValue(i, 2)) / (allRankedGenes.getRowCount() / 2));
			}
			
			
			// We add two extra points to ensure the plot starts and ends
			// at zero
			AnnotationMatrix upM = 
					AnnotatableMatrix.createNumericalMatrix(geneSetCrossingIndex + 2, 2);
			
			upM.setColumnName(0, "GSEA Up x");
			upM.setColumnName(1, "GSEA Up y");
			
			// Set the y = 0 end points
			upM.set(0, 0, 1);
			upM.set(0, 1, 0);
			upM.set(upM.getRowCount() - 1, 0, crossingX);
			upM.set(upM.getRowCount() - 1, 1, 0);
			
			for (int i = 0; i < geneSetCrossingIndex; ++i) {
				upM.set(i + 1, 0, m.get(i, 0));
				upM.set(i + 1, 1, m.get(i, 1));
			}
			
			
			
			if (es >= 0) {
				color = RED_COLOR;
			} else {
				color = Color.GRAY;
			}
			
			XYSeries series = new XYSeries("GSEA Up", color);
			series.getStyle().getFillStyle().setColor(ColorUtils.getTransparentColor70(color));
			series.getStyle().getLineStyle().setColor(color);
			series.getMarkerStyle().setVisible(false);
			
			PlotFactory.createFilledLinePlot(upM, axes, series);
			
			
			AnnotationMatrix downM = 
					AnnotatableMatrix.createNumericalMatrix(m.getRowCount() - geneSetCrossingIndex + 2, 2);
			
			downM.setColumnName(0, "GSEA Down x");
			downM.setColumnName(1, "GSEA Down y");
			
			downM.set(0, 0, crossingX);
			downM.set(0, 1, 0);
			downM.set(downM.getRowCount() - 1, 0, allRankedGenes.getRowCount());
			downM.set(downM.getRowCount() - 1, 1, 0);
			
			for (int i = 0; i < m.getRowCount() - geneSetCrossingIndex; ++i) {
				
				downM.set(i + 1, 0, m.getValue(i + geneSetCrossingIndex, 0));
				downM.set(i + 1, 1, m.getValue(i + geneSetCrossingIndex, 1));
				
				//System.err.println(i + " " + downM.getValue(i + 1, 0) + " " + + downM.getValue(i + 1, 1));
				
			}
			
			if (es < 0) {
				color = BLUE_COLOR;
			} else {
				color = Color.GRAY;
			}
			
			series = new XYSeries("GSEA Down", color);
			series.getStyle().getFillStyle().setColor(ColorUtils.getTransparentColor70(color));
			series.getStyle().getLineStyle().setColor(color);
			series.getMarkerStyle().setVisible(false);
			
			PlotFactory.createFilledLinePlot(downM, axes, series);

			

			
			//
			// The leading edge
			//
			
			int ls = Integer.MAX_VALUE;
			int le = Integer.MIN_VALUE;
			
			for (int i = 0; i < m.getRowCount(); ++i) {
				if (m.getText(i, 2).equals("Yes")) {
					ls = Math.min(ls, i);
					le = Math.max(le, i);
				}
			}
			
			int ld = le - ls + 1;
			
			AnnotationMatrix leadingM = 
					AnnotatableMatrix.createAnnotatableMatrix(ld + 1, 2);
			
			leadingM.setColumnName(0, "Leading x");
			leadingM.setColumnName(1, "Leading y");
			
			if (ls == 0) {
				// up
				color = RED_COLOR;
				
				leadingM.set(0, 0, 0);
				leadingM.set(0, 1, 0);
				
				for (int i = 0; i < ld; ++i) {	
					leadingM.set(i + 1, 0, m.get(ls + i, 0));
					leadingM.set(i + 1, 1, m.get(ls + i, 1));
				}
			} else {
				// down
				color = BLUE_COLOR;
				
				for (int i = 0; i < ld; ++i) {	
					leadingM.set(i, 0, m.get(ls + i, 0));
					leadingM.set(i, 1, m.get(ls + i, 1));
				}
				
				leadingM.set(leadingM.getRowCount() - 1, 0, allRankedGenes.getRowCount());
				leadingM.set(leadingM.getRowCount() - 1, 1, 0);
			}
			
			series = new XYSeries("Leading", color);
			series.getStyle().getFillStyle().setColor(ColorUtils.getTransparentColor70(color));
			series.getStyle().getLineStyle().setVisible(false);
			series.getMarkerStyle().setVisible(false);
			
			PlotFactory.createFilledTrapezoidPlot(leadingM, axes, series);
			
			//
			// Set some plot properties
			//
			
			
			axes.setY1AxisLimitAutoRound();
			axes.setInternalSize(DEFAULT_WIDTH, 500);
			axes.setMargins(100);
			
			Plot plot = axes.newPlot();
			plot.addChild(new LabelPlotLayer(p1, 0, 0, 10, -10));
			plot.addChild(new RightLabelPlotLayer(p2, allRankedGenes.getRowCount(), 0, -10, -10));

			
			plot.addChild(new LabelPlotLayer("Size:", allRankedGenes.getRowCount(), 0, -200, -120));
			plot.addChild(new LabelPlotLayer(Integer.toString(sizeMap.get(name)), allRankedGenes.getRowCount(), 0, -100, -120));
			//plot.getPlotLayerZModel().setZ(new LabelPlotLayer("ES:", allRankedGenes.getRowCount(), 0, -200, -140));
			//plot.getPlotLayerZModel().setZ(new LabelPlotLayer(TextUtils.format4DP(es), allRankedGenes.getRowCount(), 0, -100, -140));
			plot.addChild(new LabelPlotLayer("NES:", allRankedGenes.getRowCount(), 0, -200, -100));
			plot.addChild(new LabelPlotLayer(Formatter.number().dp(4).format(nesMap.get(name)), allRankedGenes.getRowCount(), 0, -100, -100));
			//plot.getPlotLayerZModel().setZ(new LabelPlotLayer("P:", allRankedGenes.getRowCount(), 0, -200, -100));
			//plot.getPlotLayerZModel().setZ(new LabelPlotLayer(TextUtils.format4DP(pMap.get(name)), allRankedGenes.getRowCount(), 0, -100, -100));
			plot.addChild(new LabelPlotLayer("FDR:", allRankedGenes.getRowCount(), 0, -200, -80));
			plot.addChild(new LabelPlotLayer(Formatter.number().dp(4).format(fdrMap.get(name)), allRankedGenes.getRowCount(), 0, -100, -80));
			
			// Plot the limits as if all genes are present
			axes.getX1Axis().setLimitsAutoRound(0, allRankedGenes.getRowCount());
			axes.getX1Axis().getTitle().setText("Gene List Index");
			axes.getY1Axis().getTitle().setText("Running Enrichment Score");
			axes.getY1Axis().setShowZerothLine(true);
			axes.getTitle().setText(name);
			
			//
			// Ranked ranked genes
			//
			
			subFigure = figure.newSubFigure();
			
			axes = subFigure.newAxes();

			PlotFactory.createHeatMap(rankedM, subFigure, axes);
			
			// Create a custom heatmap
			
			ColorMap colorMap = ColorMap.createFourColorMap("blue_red", 
					BLUE_COLOR,
					BLUE_COLOR_2,
					RED_COLOR_2,
					RED_COLOR,
					32,
					false);
			
			//colorMap.setRange(-1, 1);
			
			axes.currentPlot().setColorMap(colorMap);
			
			// Set the axes ranges etc
			
			axes.getX1Axis().setLimitsAutoRound(0, allRankedGenes.getRowCount());
			axes.getY1Axis().setLimitsAutoRound(0, 1);
			axes.setInternalSize(DEFAULT_WIDTH, 60);
			axes.setMargins(100);
			axes.getX1Axis().getTitle().setText("Gene List Index");
			Axes.disableAllFeatures(axes);
			
			//
			// All the ranked ranked genes
			//
			
		
			subFigure = figure.newSubFigure();
			
			axes = subFigure.newAxes();
			
			color = GREEN_COLOR;
			
			series = new XYSeries("Ranked", color);
			series.getStyle().getFillStyle().setColor(ColorUtils.getTransparentColor60(color));
			series.getStyle().getLineStyle().setColor(color);
			series.getMarkerStyle().setVisible(false);
			
			//PlotFactory.createSplineFilledLinePlot(allRankedGenes, axes, series);
			PlotFactory.createFilledTrapezoidLinePlot(allRankedGenes, axes, series);
			
			axes.setMargins(40, 0, 0, 0);
			axes.setInternalSize(DEFAULT_WIDTH, 200);
			axes.getX1Axis().getTitle().setText("Gene List Index");
			axes.getY1Axis().getTitle().setText("Ranked List Metric");
			Axis.disableAllFeatures(axes.getX1Axis());
			axes.getY1Axis().getGrid().setVisible(false);
			
			Graph2dWindow window = new Graph2dWindow(mParent, figure);

			window.setVisible(true);
			
			// FOR DEGUG ONL
			//break;
		}
		
		RecentFilesService.getInstance().setPwd(dir);
	}

	private static List<Path> getPlotFiles(Path dir) throws IOException {
		Path geneSetsFile = dir.resolve("gene_set_sizes.xls");
		
		// Get the names of each gene set
		List<String> names = Io.getColumn(geneSetsFile, true, 0);
		
		// Convert the gene set names into a list of files to extract data
		// from
		List<Path> plotFiles = new ArrayList<Path>(names.size());
		
		for (String name : names) {
			plotFiles.add(dir.resolve(name + ".xls"));
		}
		
		return plotFiles;
	}
}
