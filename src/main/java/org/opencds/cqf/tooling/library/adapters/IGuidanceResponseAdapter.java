package org.opencds.cqf.tooling.library.adapters;

import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Date;
import java.util.List;

public interface IGuidanceResponseAdapter {
    String getPatientId();
    String getGuidanceResponseId();
    IBaseResource getGuidanceResponse();
}
