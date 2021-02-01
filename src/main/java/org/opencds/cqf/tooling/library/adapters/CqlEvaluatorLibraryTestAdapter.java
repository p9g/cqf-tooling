package org.opencds.cqf.tooling.library.adapters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.util.BundleUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.Library;
import org.hl7.fhir.Parameters;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.opencds.cqf.cql.evaluator.guice.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.guice.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.tooling.parameter.TestIGParameters;
import org.opencds.cqf.tooling.processor.IGProcessor;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CqlEvaluatorLibraryTestAdapter extends LibraryTestAdapter {

    private String contentPath;
    private IBaseResource contentBundle;

    private LibraryProcessor libraryProcessor;

    private IBaseResource libraryEndpoint;
    private IBaseResource terminologyEndpoint;
    private IBaseResource dataEndpoint;

    private VersionedIdentifier id;

    /// This guy will eventually run the the cql-evaluator to get the results...
    public CqlEvaluatorLibraryTestAdapter(FhirContext fhirContext, IBaseResource testBundle, IBaseResource contentBundle, TestIGParameters params) {
        super(fhirContext, testBundle, params);

        this.contentBundle = contentBundle;

        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());
        this.libraryProcessor = injector.getInstance(LibraryProcessor.class);
    }

    public CqlEvaluatorLibraryTestAdapter(FhirContext fhirContext, String testPath, String contentPath, TestIGParameters params) {
        super(fhirContext, testPath, params);

        this.contentPath = contentPath;

        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());
        libraryProcessor = injector.getInstance(LibraryProcessor.class);
    }

    @Override
    public IParameterAdapter getActualParameterAdapter() {
        return evaluate();
    }

    @Override
    protected IParameterAdapter evaluate() {
        // Generate or get Guidance Response Resource
        IGuidanceResponseAdapter guidanceResponseAdapter = this.getExpectedGuidanceResponseAdapter();

        // Get Patient
        String subject = guidanceResponseAdapter.getSubject().getIdElement().getIdPart();

        String contentPath = this.contentPath;
        IBaseResource contentBundle = this.contentBundle;
        IBaseResource testBundle = this.testBundle;
        String testPath = this.testPath;

        // Create a Library Endpoint (with an address to cql filepath - input/pagecontent/cql)
        this.libraryEndpoint = createEndpoint(this.params.rootDir + "/" + IGProcessor.cqlLibraryPathElement, "hl7-cql-files");

        // Create a Versioned Identifier with the ID as the CQL filename / id and versioned.
        String libraryName = "library-" + StringUtils.removeEnd(contentBundle.getIdElement().getIdPart(), "-bundle");
        this.id = getLibraryIdentifier(libraryName, contentBundle);

        // Create a Terminology Endpoint (with an address to valueset filepath - input/vocabulary/valueset)
        this.terminologyEndpoint = createEndpoint(this.params.rootDir + "/" + IGProcessor.valuesetsPathElement, "hl7-fhir-files");

        // Create a data Endpoint (with an address to test case filepath - input/tests/library/<test_case>) or use the test bundle as additional data?
        String testCasePath = StringUtils.removeEnd(StringUtils.removeStart(testBundle.getIdElement().getIdPart(), "tests-"), "-bundle");
        this.dataEndpoint = createEndpoint(this.params.rootDir + "/" + IGProcessor.testCasePathElement + "library/" + StringUtils.removeStart(libraryName, "library-") + "/" + testCasePath, "hl7-fhir-files");

        org.hl7.fhir.r4.model.Parameters actual = (org.hl7.fhir.r4.model.Parameters)
                libraryProcessor.evaluate(this.id, subject, null, this.libraryEndpoint, this.terminologyEndpoint, this.dataEndpoint, null, null);
        //Parameters actual = (Parameters) libraryProcessor.evaluate(id, expected.getSubject(), null, )

        // ex: Parameters actual = (Parameters)libraryProcessor.evaluate(id,
        //        this.getSubject(test), null, libraryEndpoint, terminologyEndpoint, dataEndpoint, null, null);

        throw new NotImplementedException();
    }

    private VersionedIdentifier getLibraryIdentifier(String libraryName, IBaseResource contentBundle) {
        if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
            org.hl7.fhir.dstu3.model.Library library = (org.hl7.fhir.dstu3.model.Library) BundleUtil.toListOfResourcesOfType(this.fhirContext, (IBaseBundle) contentBundle,
                    this.fhirContext.getResourceDefinition("Library").getImplementingClass()).stream().filter(x -> x.getIdElement().getIdPart().equals(libraryName)).findFirst().get();
            return new VersionedIdentifier().withId(StringUtils.removeStart(library.getIdElement().getIdPart(), "library-")).withVersion(library.getVersion());
        } else if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R4) {
            org.hl7.fhir.r4.model.Library library = (org.hl7.fhir.r4.model.Library) BundleUtil.toListOfResourcesOfType(this.fhirContext, (IBaseBundle) contentBundle,
                this.fhirContext.getResourceDefinition("Library").getImplementingClass()).stream().filter(x -> x.getIdElement().getIdPart().equals(libraryName)).findFirst().get();
            return new VersionedIdentifier().withId(StringUtils.removeStart(library.getIdElement().getIdPart(), "library-")).withVersion(library.getVersion());
        }
        else {
            throw new NotImplementedException();
        }
    }

    private IBaseResource createEndpoint(String url, String type) {
        url = url.substring(0, url.length() - 1);
        if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU3) {
            return new org.hl7.fhir.dstu3.model.Endpoint().setAddress(url).setConnectionType(new org.hl7.fhir.dstu3.model.Coding().setCode(type));
        } else if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R4) {
            return new org.hl7.fhir.r4.model.Endpoint().setAddress((url)).setConnectionType(new org.hl7.fhir.r4.model.Coding().setCode(type));
        }
        else {
            throw new NotImplementedException();
        }
    }

    private Set<String> asSet(String... strings) {
        Set<String> set = new HashSet<>();
        for (String s : strings) {
            set.add(s);
        }

        return set;
    }
}
