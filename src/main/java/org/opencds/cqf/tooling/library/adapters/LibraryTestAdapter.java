package org.opencds.cqf.tooling.library.adapters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.util.BundleUtil;
import org.apache.commons.lang.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.utilities.IOUtils;

import java.util.List;
import java.util.Objects;

public abstract class LibraryTestAdapter {

    protected String testPath;
    protected FhirContext fhirContext;
    protected IBaseResource testBundle;
    protected IGuidanceResponseAdapter expectedGuidanceAdapter;
    protected IParameterAdapter expectedParameter;
    protected IParameterAdapter actualParameter;
    protected IBaseResource expectedGuidanceResponse;

    public LibraryTestAdapter(FhirContext fhirContext, String testPath) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null.");
        Objects.requireNonNull(testPath, "testPath can not be null.");

        this.testBundle = IOUtils.readResource(testPath, fhirContext);

        if (testBundle == null) {
            throw new IllegalArgumentException(String.format("FHIR Resource does not exist at %s", testPath));
        }

        validateTestBundle();
        this.expectedGuidanceAdapter = getGuidanceResponseAdapter(this.expectedGuidanceResponse);
    }

    public LibraryTestAdapter(FhirContext fhirContext, IBaseResource testBundle) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null.");
        this.testBundle = Objects.requireNonNull(testBundle, "testBundle can not be null.");

        validateTestBundle();
        this.expectedGuidanceAdapter = getGuidanceResponseAdapter(this.expectedGuidanceResponse);
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

        List<? extends IBaseResource> guidanceResponses = BundleUtil.toListOfResourcesOfType(this.fhirContext, bundle,
                this.fhirContext.getResourceDefinition("GuidanceResponse").getImplementingClass());

        // Get Expected Result GR
        if (guidanceResponses == null || guidanceResponses.size() == 0) {

            // Expected Result GR does not exist. Generate it.
        }
        else {
            this.expectedGuidanceResponse = guidanceResponses.get(0);
        }
    }

    protected abstract IParameterAdapter evaluate();

    public abstract IParameterAdapter getActualParameterAdapter();

    public IParameterAdapter getExpectedParameterAdapter() {
        return this.expectedParameter;
    }

    public IGuidanceResponseAdapter getExpectedGuidanceResponseAdapter() { return this.expectedGuidanceAdapter; }
}



