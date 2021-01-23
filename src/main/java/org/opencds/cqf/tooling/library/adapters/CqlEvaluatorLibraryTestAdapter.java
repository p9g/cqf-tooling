package org.opencds.cqf.tooling.library.adapters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.guice.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.guice.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;

public class CqlEvaluatorLibraryTestAdapter extends LibraryTestAdapter {

    private String contentPath;
    private IBaseResource contentBundle;
    private LibraryProcessor libraryProcessor;

    /// This guy will eventually run the the cql-evaluator to get the results...
    public CqlEvaluatorLibraryTestAdapter(FhirContext fhirContext, IBaseResource testBundle, IBaseResource contentBundle) {
        super(fhirContext, testBundle);

        this.contentBundle = contentBundle;

        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());
        libraryProcessor = injector.getInstance(LibraryProcessor.class);
    }

    public CqlEvaluatorLibraryTestAdapter(FhirContext fhirContext, String testPath, String contentPath) {
        super(fhirContext, testPath);

        this.contentPath = contentPath;

        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());
        libraryProcessor = injector.getInstance(LibraryProcessor.class);
    }

    @Override
    public IParameterAdapter getActualParameterAdapter() {
        throw new NotImplementedException();
    }

    @Override
    protected IParameterAdapter evaluate() {
        // 1. Get Gr/Parameter and Patient Ids from Expected
        // 2. Run evaluator with PlanDefinition, Patient, Content context
        // 3. Parse the result

        // ex: Parameters actual = (Parameters)libraryProcessor.evaluate(id,
        //        this.getSubject(test), null, libraryEndpoint, terminologyEndpoint, dataEndpoint, null, asSet("TotalMME"));

        throw new NotImplementedException();
    }
}
