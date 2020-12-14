package org.opencds.cqf.tooling.plandefinition.adapters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.plandefinition.adapters.IGuidanceResponseAdapter;
import org.opencds.cqf.tooling.plandefinition.adapters.PlanDefinitionTestAdapter;
import org.opencds.cqf.cql.evaluator.guice.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.guice.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.w3._1999.xhtml.Li;

public class CqlEvaluatorPlanDefinitionTestAdapter extends PlanDefinitionTestAdapter {

    private String contentPath;
    private IBaseResource contentBundle;
    private LibraryProcessor libraryProcessor;

    /// This guy will eventually run the the cql-evaluator to get the results...
    public CqlEvaluatorPlanDefinitionTestAdapter(FhirContext fhirContext, IBaseResource testBundle, IBaseResource contentBundle) {
        super(fhirContext, testBundle);

        this.contentBundle = contentBundle;

        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());
        libraryProcessor = injector.getInstance(LibraryProcessor.class);
    }

    public CqlEvaluatorPlanDefinitionTestAdapter(FhirContext fhirContext, String testPath, String contentPath) {
        super(fhirContext, testPath);

        this.contentPath = contentPath;

        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());
        libraryProcessor = injector.getInstance(LibraryProcessor.class);
    }

    @Override
    public IParameterAdapter getActualParameterAdapter() {
        // 1. Get Gr/Parameter and Patient Ids from Expected
        // 2. Run evaluator with PlanDefinition, Patient, Content context
        // 3. Parse the result

        throw new NotImplementedException();
    }

    @Override
    protected IParameterAdapter evaluate() {
        throw new NotImplementedException();
    }
}
