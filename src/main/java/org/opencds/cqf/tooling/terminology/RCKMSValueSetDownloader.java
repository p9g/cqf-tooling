package org.opencds.cqf.tooling.terminology;

import java.util.ArrayList;
import java.util.List;

import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.tooling.utilities.ResourceUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class RCKMSValueSetDownloader {

    public List<String> getReferencedValueSets(String cqlContentPath) {
        List<String> valueSetUrls = new ArrayList<>();

        Library library = ResourceUtils.getElmFromCql(cqlContentPath);

        // TODO: Use $data-requirements when that's done.
        if (library.getValueSets() != null && library.getValueSets().getDef() != null) {
            for (ValueSetDef def : library.getValueSets().getDef()) {
                valueSetUrls.add(def.getId());
            }
        }

        return valueSetUrls;
    }

    public void download(String cqlContentPath, String outputDirectory) {
        // Get the set of ValueSets the CQL uses
        List<String> valueSetUrls = this.getReferencedValueSets(cqlContentPath);
        
        // Download each ValueSet from RCKMS
        List<ValueSet> valueSets = this.downloadValueSets(valueSetUrls);

        // Write out the ValueSets to a directory
        this.writeValueSets(valueSets, outputDirectory);
    }

    public List<ValueSet> downloadValueSets(List<String> valueSetUrls) {
        List<ValueSet> valueSets = new ArrayList<>();

        // Create an HTTP client and authenticate with the RCKMS servers
        for (String url : valueSetUrls) {
            // Download ValueSet and add it to the list.
        }

        return valueSets;
    }

    public void writeValueSets(List<ValueSet> valueSets, String outputDirectory) {
        IParser parser = FhirContext.forR4().newJsonParser();

        // for each valueset, write out the value set in the outputDirectory as "id.json/xml"
    }
    
}
