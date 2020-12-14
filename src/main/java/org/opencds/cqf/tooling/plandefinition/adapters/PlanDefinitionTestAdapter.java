package org.opencds.cqf.tooling.plandefinition.adapters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.util.BundleUtil;
import org.apache.commons.lang.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.measure.adapters.IMeasureReportAdapter;
import org.opencds.cqf.tooling.plandefinition.adapters.*;
import org.opencds.cqf.tooling.utilities.IOUtils;

import java.util.List;
import java.util.Objects;

public abstract class PlanDefinitionTestAdapter {

    protected String testPath;
    protected FhirContext fhirContext;
    protected IBaseResource testBundle;
    protected IGuidanceResponseAdapter expectedGuidanceAdapter;
    protected IParameterAdapter expectedParameterAdapter;
    protected IParameterAdapter actualParameterAdapter;

    private IBaseResource expectedParameter;

    public PlanDefinitionTestAdapter(FhirContext fhirContext, String testPath) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null.");
        Objects.requireNonNull(testPath, "testPath can not be null.");

        this.testBundle = IOUtils.readResource(testPath, fhirContext);

        if (testBundle == null) {
            throw new IllegalArgumentException(String.format("FHIR Resource does not exist at %s", testPath));
        }

        validateTestBundle();
        this.expectedGuidanceAdapter = getGuidanceResponseAdapter(this.expectedParameter);
    }

    public PlanDefinitionTestAdapter(FhirContext fhirContext, IBaseResource testBundle) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null.");
        this.testBundle = Objects.requireNonNull(testBundle, "testBundle can not be null.");

        validateTestBundle();
        this.expectedGuidanceAdapter = getGuidanceResponseAdapter(this.expectedParameter);
    }

    protected IGuidanceResponseAdapter getGuidanceResponseAdapter(IBaseResource guidanceResponse) {
        //TODO: R5?
        IGuidanceResponseAdapter guidanceResponseAdapter;
        if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
            throw new NotImplementedException();
        } else if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R4) {
            guidanceResponseAdapter = new R4GuidanceResponseAdapter((org.hl7.fhir.r4.model.GuidanceResponse)guidanceResponse);
        } else {
            throw new IllegalArgumentException("Unsupported or unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
        }

        return guidanceResponseAdapter;
    }

    private void validateTestBundle() {
        if (this.testBundle == null) {
            throw new IllegalArgumentException("testBundle can not be null");
        }

        if (!this.testBundle.fhirType().equals("Bundle") || !(this.testBundle instanceof IBaseBundle)) {
            throw new IllegalArgumentException("testBundle is not a Bundle Resource");
        }

        IBaseBundle bundle = (IBaseBundle)this.testBundle;

        List<? extends IBaseResource> parameters = BundleUtil.toListOfResourcesOfType(this.fhirContext, bundle,
                this.fhirContext.getResourceDefinition("Parameter").getImplementingClass());

        if (parameters == null || parameters.size() == 0) {
            throw new IllegalArgumentException("Bundle is not a valid Plan Definition Test Bundle. It must contain an Input Parameters Resource.");
        }

        this.expectedParameter = parameters.get(0);
    }

    protected abstract IParameterAdapter evaluate();

    public abstract IParameterAdapter getActualParameterAdapter();

    public IParameterAdapter getExpectedParameterAdapter() {
        return this.expectedParameterAdapter;
    }
}



