package org.opencds.cqf.tooling.plandefinition;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang.NotImplementedException;
import org.hl7.fhir.Parameters;
import org.hl7.fhir.ParametersParameter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.measure.adapters.IMeasureReportAdapter;
import org.opencds.cqf.tooling.measure.adapters.MeasureTestAdapter;
import org.opencds.cqf.tooling.measure.comparer.MeasureReportComparer;
import org.opencds.cqf.tooling.plandefinition.adapters.*;
import org.opencds.cqf.tooling.processor.ITestProcessor;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class PlanDefinitionTestProcessor implements ITestProcessor {

    private FhirContext fhirContext;

    public PlanDefinitionTestProcessor(FhirContext fhirContext)
    {
        this.fhirContext = fhirContext;
    }

    public Parameters executeTest(String testPath, String contentBundlePath, String fhirServer)
    {
        throw new NotImplementedException();
    }

    public Parameters executeTest(IBaseResource testBundle, IBaseResource contentBundle, String fhirServer)
    {
        // Get the correct Testing Adapter
        PlanDefinitionTestAdapter adapter = getPlanDefinitionTestAdapter(testBundle, contentBundle, fhirServer);
        IParameterAdapter expected = adapter.getExpectedParameterAdapter();

        // Get the Input Parameters resource for the Test Case

        // Generate a Guidance Response Resource

        // Generate a Output Parameters resource

        // Compare the Input and Output Parameter Resources
//        MeasureReportComparer comparer = new MeasureReportComparer(this.fhirContext);

        // Log and Return results
//        Parameters results = comparer.compare(actual, expected);
//        logTestResults(measureId, results);
//        return results;




//        String measureId = expected.getMeasureId();
//        System.out.println("            Testing Plan  '" + measureId + "'");

        throw new NotImplementedException();
    }

    private void logTestResults(String artifactId, Parameters results) {
        //TODO: Can do whatever we want here, just printing to out for now - just hacked together console output.
        System.out.println("            Test results for Plan Definition '" + artifactId + "':");
        for (ParametersParameter parameter : results.getParameter()) {
            String assertionString = "";

            if (parameter.getName().getValue().indexOf(TestPassedKey) >= 0) {
                assertionString = ": ";
            }
            else {
                assertionString = " matched expected value: ";
            }
            System.out.println("            " + parameter.getName().getValue() + assertionString + parameter.getValueBoolean().isValue().toString());
        }
    }

    public PlanDefinitionTestAdapter getPlanDefinitionTestAdapter(IBaseResource testBundle, IBaseResource contentBundle, String fhirServer) {
        Objects.requireNonNull(testBundle, "            testBundle can not be null");

        if ((fhirServer == null || fhirServer.trim().isEmpty()) && (contentBundle == null)) {
            throw new IllegalArgumentException("If fhirServer is not specified, contentBundle can not be null or empty.");
        }

        if (fhirServer == null) {
            return new CqlEvaluatorPlanDefinitionTestAdapter(this.fhirContext, testBundle, contentBundle);
        }
        else {
            throw new NotImplementedException();
        }
    }

    public PlanDefinitionTestAdapter getPlanDefinitionTestAdapter(String testPath, String contentBundlePath, String fhirServer) {
        Objects.requireNonNull(testPath, "          testPath can not be null");

        File testFile = new File(testPath);
        if(!testFile.exists())
        {
            throw new IllegalArgumentException(String.format("          testPath file not found: %s", testPath));
        }

        if ((fhirServer == null || fhirServer.trim().isEmpty()) && (contentBundlePath == null || contentBundlePath.trim().isEmpty())) {
            throw new IllegalArgumentException("If fhirServer is not specified, contentBundlePath can not be null.");
        }

        if (fhirServer == null) {
            return new CqlEvaluatorPlanDefinitionTestAdapter(this.fhirContext, testPath, contentBundlePath);
        }
        else {
            throw new NotImplementedException();
        }
    }
}


