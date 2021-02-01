package org.opencds.cqf.tooling.library.adapters;

import org.apache.commons.lang.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.GuidanceResponse;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;

public class R4ParametersAdapter implements IParameterAdapter {
    private org.hl7.fhir.r4.model.Parameters parameters;

    public R4ParametersAdapter(Parameters parameters) { this.parameters = parameters; }

    @Override
    public IBaseResource getParameters() { return this.parameters; }
}
