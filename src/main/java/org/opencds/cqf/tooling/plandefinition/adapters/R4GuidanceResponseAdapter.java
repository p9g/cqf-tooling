package org.opencds.cqf.tooling.plandefinition.adapters;

import org.hl7.fhir.r4.model.GuidanceResponse;

public class R4GuidanceResponseAdapter implements IGuidanceResponseAdapter {
    private org.hl7.fhir.r4.model.GuidanceResponse guidanceResponse;
    public R4GuidanceResponseAdapter(GuidanceResponse guidanceResponse) {
        this.guidanceResponse = guidanceResponse;
    }

    //TODO: In R4 the Subject will not necessarily be a Patient.
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
}
