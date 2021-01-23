package org.opencds.cqf.tooling.processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.fhir.ucum.UcumService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.opencds.cqf.tooling.npm.LibraryLoader;
import org.opencds.cqf.tooling.parameter.RefreshLibraryParameters;
import org.opencds.cqf.tooling.utilities.*;
import org.opencds.cqf.tooling.utilities.IOUtils.Encoding;

import ca.uhn.fhir.context.FhirContext;

public class LibraryProcessor extends BaseProcessor {
    public static final String ResourcePrefix = "library-";
    public static final String LibraryTestGroupName = "library";
    public static String getId(String baseId) {
        return ResourcePrefix + baseId;
    }
    
    public static List<String> refreshIgLibraryContent(BaseProcessor parentContext, Encoding outputEncoding, Boolean versioned, FhirContext fhirContext) {
        System.out.println("Refreshing libraries...");
        ArrayList<String> refreshedLibraryNames = new ArrayList<String>();

        LibraryProcessor libraryProcessor;
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                libraryProcessor = new STU3LibraryProcessor();
                break;
            case R4:
                libraryProcessor = new R4LibraryProcessor();
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
        }

        String libraryPath = FilenameUtils.concat(parentContext.rootDir, IGProcessor.libraryPathElement);
        RefreshLibraryParameters params = new RefreshLibraryParameters();
        params.libraryPath = libraryPath;
        params.parentContext = parentContext;
        params.fhirContext = fhirContext;
        params.encoding = outputEncoding;
        params.versioned = versioned;
        return libraryProcessor.refreshLibraryContent(params);
    }

    public static Boolean bundleLibraryDependencies(String path, FhirContext fhirContext, Map<String, IBaseResource> resources,
            Encoding encoding, boolean versioned) {
        Boolean shouldPersist = true;
        try {
            Map<String, IBaseResource> dependencies = ResourceUtils.getDepLibraryResources(path, fhirContext, encoding, versioned);
            String currentResourceID = IOUtils.getTypeQualifiedResourceId(path, fhirContext);
            for (IBaseResource resource : dependencies.values()) {
                resources.putIfAbsent(resource.getIdElement().getIdPart(), resource);

                // NOTE: Assuming dependency library will be in directory of dependent.
                String dependencyPath = IOUtils.getResourceFileName(IOUtils.getResourceDirectory(path), resource, encoding, fhirContext, versioned);
                bundleLibraryDependencies(dependencyPath, fhirContext, resources, encoding, versioned);
            }
        } catch (Exception e) {
            shouldPersist = false;
            LogUtils.putException(path, e);
        }
        return shouldPersist;
    }

    private UcumService ucumService;
    private List<String> binaryPaths;
    private CqlProcessor cqlProcessor;
    protected boolean versioned;

    /*
    Refreshes generated content in the given library.
    The name element of the library resource is used to find the cql file (filename = <name>.cql)
    The CqlProcessor is used to get the CqlSourceFileInformation
    Sets
        * cqlContent
        * elmXmlContent
        * elmJsonContent
        * dataRequirements
        * relatedArtifacts
        * parameters

     Does not set publisher-level information (id, name, url, version, publisher, contact, jurisdiction)
     Does not generate narrative
     */
    protected Library refreshGeneratedContent(Library sourceLibrary) {
        String libraryName = sourceLibrary.getName();
        if (versioned) {
            libraryName += "-" + sourceLibrary.getVersion();
        }
        String fileName = libraryName + ".cql";
        Attachment attachment = null;
        try {
            attachment = loadFile(fileName);
        } catch (IOException e) {
            logMessage(String.format("Error loading CQL source for library %s", libraryName));
            e.printStackTrace();
        }

        if (attachment != null) {
            sourceLibrary.getContent().clear();
            sourceLibrary.getContent().add(attachment);
            CqlProcessor.CqlSourceFileInformation info = cqlProcessor.getFileInformation(attachment.getUrl());
            attachment.setUrlElement(null);
            if (info != null) {
                //f.getErrors().addAll(info.getErrors());
                if (info.getElm() != null) {
                    sourceLibrary.addContent().setContentType("application/elm+xml").setData(info.getElm());
                }
                if (info.getJsonElm() != null) {
                    sourceLibrary.addContent().setContentType("application/elm+json").setData(info.getJsonElm());
                }
                sourceLibrary.getDataRequirement().clear();
                sourceLibrary.getDataRequirement().addAll(info.getDataRequirements());
                sourceLibrary.getRelatedArtifact().removeIf(n -> n.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON);
                sourceLibrary.getRelatedArtifact().addAll(info.getRelatedArtifacts());
                sourceLibrary.getParameter().clear();
                sourceLibrary.getParameter().addAll(info.getParameters());
            } else {
                logMessage(String.format("No cql info found for ", fileName));
                //f.getErrors().add(new ValidationMessage(ValidationMessage.Source.Publisher, ValidationMessage.IssueType.NOTFOUND, "Library", "No cql info found for "+f.getName(), ValidationMessage.IssueSeverity.ERROR));
            }
        }

        return sourceLibrary;
    }

    protected List<Library> refreshGeneratedContent(List<Library> sourceLibraries) {
        try {
            binaryPaths = IGUtils.extractBinaryPaths(rootDir, sourceIg);
        }
        catch (IOException e) {
            logMessage(String.format("Errors occurred extracting binary path from IG: ", e.getMessage()));
            throw new IllegalArgumentException("Could not obtain binary path from IG");
        }

        LibraryLoader reader = new LibraryLoader(fhirVersion);
        try {
            ucumService = new UcumEssenceService(UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
        } catch (UcumException e) {
            System.err.println("Could not create UCUM validation service:");
            e.printStackTrace();
        }
        cqlProcessor = new CqlProcessor(packageManager.getNpmList(), binaryPaths, reader, this, ucumService,
                packageId, canonicalBase);

        return internalRefreshGeneratedContent(sourceLibraries);
    }

    public List<Library> refreshGeneratedContent(String cqlDirectoryPath, String fhirVersion) {
        List<String> result = new ArrayList<String>();
        File input = new File(cqlDirectoryPath);
        if (input.exists() && input.isDirectory()) {
            result.add(input.getAbsolutePath());
        }
        binaryPaths = result;

        LibraryLoader reader = new LibraryLoader(fhirVersion);
        try {
            ucumService = new UcumEssenceService(UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
        } catch (UcumException e) {
            System.err.println("Could not create UCUM validation service:");
            e.printStackTrace();
        }
        cqlProcessor = new CqlProcessor(null, binaryPaths, reader, this, ucumService,
                null, null);
        List<Library> libraries = new ArrayList<Library>();
        return internalRefreshGeneratedContent(libraries);
    }

    private List<Library> internalRefreshGeneratedContent(List<Library> sourceLibraries) {
        cqlProcessor.execute();

        // For each CQL file, ensure that there is a Library resource with a matching name and version
        for (CqlProcessor.CqlSourceFileInformation fileInfo : cqlProcessor.getAllFileInformation()) {
            if (fileInfo.getIdentifier() != null && fileInfo.getIdentifier().getId() != null && !fileInfo.getIdentifier().getId().equals("")) {
                Library existingLibrary = null;
                for (Library sourceLibrary : sourceLibraries) {
                    if (fileInfo.getIdentifier().getId().equals(sourceLibrary.getName())
                            && (fileInfo.getIdentifier().getVersion() == null || fileInfo.getIdentifier().getVersion().equals(sourceLibrary.getVersion()))
                    ) {
                        existingLibrary = sourceLibrary;
                        break;
                    }
                }

                if (existingLibrary == null) {
                    Library newLibrary = new Library();
                    newLibrary.setName(fileInfo.getIdentifier().getId());
                    newLibrary.setVersion(fileInfo.getIdentifier().getVersion());
                    newLibrary.setUrl(String.format("%s/Library/%s", (newLibrary.getName().equals("FHIRHelpers") ? "http://hl7.org/fhir" : canonicalBase), fileInfo.getIdentifier().getId()));
                    newLibrary.setId(LibraryProcessor.getId(newLibrary.getName()) + (versioned ? "-" + newLibrary.getVersion() : ""));
                    sourceLibraries.add(newLibrary);
                }
            }
        }

        List<Library> resources = new ArrayList<Library>();
        for (Library library : sourceLibraries) {
            resources.add(refreshGeneratedContent(library));
        }
        return resources;
    }

    private Attachment loadFile(String fn) throws IOException {
        for (String dir : binaryPaths) {
            File f = new File(Utilities.path(dir, fn));
            if (f.exists()) {
                Attachment att = new Attachment();
                att.setContentType("text/cql");
                att.setData(TextFile.fileToBytes(f));
                att.setUrl(f.getAbsolutePath());
                return att;
            }
        }
        return null;
    }

    public static void bundleTestLibraries(ArrayList<String> refreshedLibraryNames, String igPath, Boolean includeDependencies,
                                      Boolean includeTerminology, Boolean includePatientScenarios, Boolean includeVersion, FhirContext fhirContext, String fhirUri,
                                      Encoding encoding) {

        List<String> refreshedTestLibraryNames = new ArrayList<String>();
        refreshedTestLibraryNames.addAll(refreshedLibraryNames);

        HashSet<String> planDefinitionSourcePaths = IOUtils.getPlanDefinitionPaths(fhirContext);

        List<String> planDefinitionPathLibraryNames = new ArrayList<String>();
        for (String planDefinitionSourcePath : planDefinitionSourcePaths) {
            String name = FilenameUtils.getBaseName(planDefinitionSourcePath).replace(PlanDefinitionProcessor.ResourcePrefix, "");

            planDefinitionPathLibraryNames.add(name);
        }

        HashSet<String> measureSourcePaths = IOUtils.getMeasurePaths(fhirContext);

        List<String> measurePathLibraryNames = new ArrayList<String>();
        for (String measureSourcePath : measureSourcePaths) {
            String name = FilenameUtils.getBaseName(measureSourcePath).replace(MeasureProcessor.ResourcePrefix, "");

            measurePathLibraryNames.add(name);
        }

        // Gather Test Libraries -- Refreshed Library Artifacts with test cases & no associated Measure or Plan Definition Artifacts.
        for (String refreshedLibraryName : refreshedLibraryNames) {
            String igTestCasePath = FilenameUtils.concat(FilenameUtils.concat(FilenameUtils.concat(igPath, IGProcessor.testCasePathElement), LibraryTestGroupName), refreshedLibraryName);
            List<IBaseResource> testCaseResources = TestCaseProcessor.getTestCaseResources(igTestCasePath, fhirContext);

            if (testCaseResources.isEmpty()) {
                refreshedTestLibraryNames.remove(refreshedLibraryName);
            }
        }

        // Process Test Libraries
        List<String> bundledTestLibraries = new ArrayList<String>();
        for (String refreshedTestLibraryName : refreshedTestLibraryNames) {
            try {
                Map<String, IBaseResource> resources = new HashMap<String, IBaseResource>();

                String refreshedLibraryFileName = IOUtils.formatFileName(refreshedTestLibraryName, encoding, fhirContext);
                String librarySourcePath;
                try {
                    librarySourcePath = IOUtils.getLibraryPathAssociatedWithCqlFileName(refreshedLibraryFileName, fhirContext);
                } catch (Exception e) {
                    LogUtils.putException(refreshedTestLibraryName, e);
                    continue;
                } finally {
                    LogUtils.warn(refreshedTestLibraryName);
                }

                Boolean shouldPersist = true;
                shouldPersist = shouldPersist
                        & ResourceUtils.safeAddResource(librarySourcePath, resources, fhirContext);

                String cqlFileName = IOUtils.formatFileName(refreshedTestLibraryName, Encoding.CQL, fhirContext);
                List<String> cqlLibrarySourcePaths = IOUtils.getCqlLibraryPaths().stream()
                        .filter(path -> path.endsWith(cqlFileName))
                        .collect(Collectors.toList());
                String cqlLibrarySourcePath = (cqlLibrarySourcePaths.isEmpty()) ? null : cqlLibrarySourcePaths.get(0);

                if (includeTerminology) {
                    boolean result = ValueSetsProcessor.bundleValueSets(cqlLibrarySourcePath, igPath, fhirContext, resources, encoding, includeDependencies, includeVersion);
                    if (shouldPersist && !result) {
                        LogUtils.info("Test Library will not be bundled because ValueSet bundling failed.");
                    }
                    shouldPersist = shouldPersist & result;
                }

                if (includeDependencies) {
                    boolean result = LibraryProcessor.bundleLibraryDependencies(librarySourcePath, fhirContext, resources, encoding, includeVersion);
                    if (shouldPersist && !result) {
                        LogUtils.info("Test Library will not be bundled because Library Dependency bundling failed.");
                    }
                    shouldPersist = shouldPersist & result;
                }

                if (includePatientScenarios) {
                    boolean result = TestCaseProcessor.bundleTestCases(igPath, LibraryTestGroupName, refreshedTestLibraryName, fhirContext, resources);
                    if (shouldPersist && !result) {
                        LogUtils.info("Test Library will not be bundled because Test Case bundling failed.");
                    }
                    shouldPersist = shouldPersist & result;
                }

                if (shouldPersist) {
                    String bundleDestPath = FilenameUtils.concat(FilenameUtils.concat(IGProcessor.getBundlesPath(igPath), LibraryTestGroupName), refreshedTestLibraryName);
                    persistBundle(igPath, bundleDestPath, refreshedTestLibraryName, encoding, fhirContext, new ArrayList<IBaseResource>(resources.values()), fhirUri);
                    bundleFiles(igPath, bundleDestPath, refreshedTestLibraryName, librarySourcePath, fhirContext, encoding, includeTerminology, includeDependencies, includePatientScenarios, includeVersion);
                    bundledTestLibraries.add(refreshedTestLibraryName);
                }

            } catch (Exception e) {
                LogUtils.putException(refreshedTestLibraryName, e);
            } finally {
                LogUtils.warn(refreshedTestLibraryName);
            }
        }

        String message = "\r\n" + bundledTestLibraries.size() + " Test Libraries successfully bundled:";
        for (String bundledTestLibrary : bundledTestLibraries) {
            message += "\r\n     " + bundledTestLibrary + " BUNDLED";
        }

        ArrayList<String> failedTestLibraries = new ArrayList<>(refreshedTestLibraryNames);
        refreshedTestLibraryNames.removeAll(bundledTestLibraries);
        refreshedTestLibraryNames.retainAll(refreshedTestLibraryNames);
        message += "\r\n" + refreshedTestLibraryNames.size() + " Test Libraries refreshed, but not bundled (due to issues):";
        for (String notBundled : refreshedTestLibraryNames) {
            message += "\r\n     " + notBundled + " REFRESHED";
        }

        failedTestLibraries.removeAll(bundledTestLibraries);
        failedTestLibraries.removeAll(refreshedTestLibraryNames);
        message += "\r\n" + failedTestLibraries.size() + " Test Libraries failed refresh:";
        for (String failed : failedTestLibraries) {
            message += "\r\n     " + failed + " FAILED";
        }

        LogUtils.info(message);
    }

    private static void persistBundle(String igPath, String bundleDestPath, String libraryName, Encoding encoding, FhirContext fhirContext, List<IBaseResource> resources, String fhirUri) {
        IOUtils.initializeDirectory(bundleDestPath);
        Object bundle = BundleUtils.bundleArtifacts(libraryName, resources, fhirContext);
        IOUtils.writeBundle(bundle, bundleDestPath, encoding, fhirContext);

        BundleUtils.postBundle(encoding, fhirContext, fhirUri, (IBaseResource) bundle);
    }

    private static void bundleFiles(String igPath, String bundleDestPath, String libraryName, String librarySourcePath, FhirContext fhirContext, Encoding encoding, Boolean includeTerminology, Boolean includeDependencies, Boolean includePatientScenarios, Boolean includeVersion) {
        String bundleDestFilesPath = FilenameUtils.concat(bundleDestPath, libraryName + "-" + IGBundleProcessor.bundleFilesPathElement);
        IOUtils.initializeDirectory(bundleDestFilesPath);

        IOUtils.copyFile(librarySourcePath, FilenameUtils.concat(bundleDestFilesPath, FilenameUtils.getName(librarySourcePath)));

        String cqlFileName = IOUtils.formatFileName(libraryName, Encoding.CQL, fhirContext);
        List<String> cqlLibrarySourcePaths = IOUtils.getCqlLibraryPaths().stream()
                .filter(path -> path.endsWith(cqlFileName))
                .collect(Collectors.toList());
        String cqlLibrarySourcePath = (cqlLibrarySourcePaths.isEmpty()) ? null : cqlLibrarySourcePaths.get(0);
        String cqlDestPath = FilenameUtils.concat(bundleDestFilesPath, cqlFileName);
        IOUtils.copyFile(cqlLibrarySourcePath, cqlDestPath);

        if (includeTerminology) {
            try {
                Map<String, IBaseResource> valuesets = ResourceUtils.getDepValueSetResources(cqlLibrarySourcePath, igPath, fhirContext, includeDependencies, includeVersion);
                if (!valuesets.isEmpty()) {
                    Object bundle = BundleUtils.bundleArtifacts(ValueSetsProcessor.getId(libraryName), new ArrayList<IBaseResource>(valuesets.values()), fhirContext);
                    IOUtils.writeBundle(bundle, bundleDestFilesPath, encoding, fhirContext);
                }
            }  catch (Exception e) {
                LogUtils.putException(libraryName, e.getMessage());
            }
        }

        if (includeDependencies) {
            Map<String, IBaseResource> depLibraries = ResourceUtils.getDepLibraryResources(librarySourcePath, fhirContext, encoding, includeVersion);
            if (!depLibraries.isEmpty()) {
                String depLibrariesID = "library-deps-" + libraryName;
                Object bundle = BundleUtils.bundleArtifacts(depLibrariesID, new ArrayList<IBaseResource>(depLibraries.values()), fhirContext);
                IOUtils.writeBundle(bundle, bundleDestFilesPath, encoding, fhirContext);
            }
        }

        if (includePatientScenarios) {
            TestCaseProcessor.bundleTestCaseFiles(igPath, "measure", libraryName, bundleDestFilesPath, fhirContext);
        }
    }

    public List<String> refreshLibraryContent(RefreshLibraryParameters params) {
        return new ArrayList<String>();
    }
}