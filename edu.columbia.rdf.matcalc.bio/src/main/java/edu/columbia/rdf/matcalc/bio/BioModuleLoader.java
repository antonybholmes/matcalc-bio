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
package edu.columbia.rdf.matcalc.bio;

import edu.columbia.rdf.matcalc.ModuleLoader;
import edu.columbia.rdf.matcalc.bio.toolbox.dna.FastaIOModule;
import edu.columbia.rdf.matcalc.bio.toolbox.external.genepattern.io.GctIOModule;
import edu.columbia.rdf.matcalc.bio.toolbox.external.genepattern.io.ResIOModule;
import edu.columbia.rdf.matcalc.bio.toolbox.external.ucsc.BedGraphIOModule;
import edu.columbia.rdf.matcalc.bio.toolbox.external.ucsc.BedIOModule;



public class BioModuleLoader extends ModuleLoader {
	public BioModuleLoader() {
		addModule(GctIOModule.class);
		addModule(ResIOModule.class);
		addModule(BedIOModule.class);
		addModule(BedGraphIOModule.class);
		addModule(FastaIOModule.class);
	}
}
