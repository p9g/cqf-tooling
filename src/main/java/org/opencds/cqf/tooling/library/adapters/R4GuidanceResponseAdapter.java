package org.opencds.cqf.tooling.library.adapters;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.GuidanceResponse;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.tooling.library.adapters.IGuidanceResponseAdapter;

public class R4GuidanceResponseAdapter implements IGuidanceResponseAdapter {
    private org.hl7.fhir.r4.model.GuidanceResponse guidanceResponse;
    private R4ParametersAdapter expectedParametersAdapter;
    private R4ParametersAdapter actualParametersAdapter;
    public R4GuidanceResponseAdapter(GuidanceResponse guidanceResponse) {
        this.guidanceResponse = guidanceResponse;

        // Setup Expected Parameters Adapter
        this.expectedParametersAdapter = new R4ParametersAdapter((Parameters) ((Reference) guidanceResponse.getExtensionByUrl("http://hl7.org/fhir/uv/cpg/StructureDefinition/inputParameters").getValue()).getResource());
    }


    @Override
    public String getPatientId() {
        String[] subjectRefParts = guidanceResponse.getSubject().getReference().split("/");
        String patientId = subjectRefParts[subjectRefParts.length - 1];
        return patientId;
    }

    @Override
    public String getGuidanceResponseId() {
        String[] guidanceRefParts = guidanceResponse.getId().split("/");
        String guidanceId = guidanceRefParts[guidanceRefParts.length - 1];
        return guidanceId;
    }

    //TODO: In R4 the Subject will not necessarily be a Patient.
    @Override
    public IBaseResource getSubject() {
        return this.guidanceResponse.getSubject().getResource();
    }

    @Override
    public IBaseResource getGuidanceResponse() { return this.guidanceResponse; }

    @Override
    public IParameterAdapter getExpectedParametersAdapter() { return this.expectedParametersAdapter; }
}
