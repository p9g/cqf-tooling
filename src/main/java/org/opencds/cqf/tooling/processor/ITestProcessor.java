package org.opencds.cqf.tooling.processor;

import org.hl7.fhir.Parameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.parameter.TestIGParameters;

import java.util.List;

public interface ITestProcessor {
    public static final String TestPassedKey  = "Test Passed";

    Parameters executeTest(String testPath, String contentBundlePath, String fhirServer);
    Parameters executeTest(IBaseResource testBundle, IBaseResource contentBundle, String fhirServer);
    Parameters executeTest(IBaseResource testBundle, IBaseResource contentBundle, TestIGParameters params);
}
