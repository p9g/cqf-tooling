package org.opencds.cqf.tooling.library.adapters;

import org.hl7.fhir.instance.model.api.IBaseResource;


public interface IParameterAdapter {
    IBaseResource getParameters();
}
