package org.opencds.cqf.tooling.operation;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.tooling.Operation;

import org.opencds.cqf.tooling.utilities.IOUtils;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResourceValidator extends Operation {

    private String resourcePath; // -resourcePath (-rp) directory to get resource(s) to evaluate
    private String profilePath = "";  // -profilePath (-pp)  directory and file path or url to get profile to validate against
    // -outputPath (-op)  directory on where to write returned results of validation, including file name.
    private String jarPath;  // -jarPath (-jp)  path to validator_cli.jar
    private String fhirVersion = "4.0.1"; // will default to fhir 4.0.1
    private String igLocation; //Location for an IG to run against or a single profile file
    private FhirContext fhirContext;
    private String profilesToUse;

    public FhirContext getFhirContext() {
        if (fhirContext == null) {
            fhirContext = FhirContext.forR4();
        }
        return fhirContext;
    }

    @Override
    public void execute(String[] args) {
        setOutputPath("src/main/resources/org/opencds/cqf/tooling/acceleratorkit/output"); // default
        for (String arg : args) {
            if (arg.equals("-ValidateResource")) continue;
            String[] flagAndValue = arg.split("=");
            if (flagAndValue.length < 2) {
                throw new IllegalArgumentException("Invalid argument: " + arg);
            }
            String flag = flagAndValue[0];
            String value = flagAndValue[1];
            switch (flag.replace("-", "").toLowerCase()) {
                case "outputpath": case "op": setOutputPath(value); break; // -outputpath (-op)
                case "profilePath": case "pp": handleProfilePath(value); break; // -profilePath (-pp) may be url
                case "ig": igLocation = value; break;
                case "resourcePath": case "rp": resourcePath = Paths.get(value).toAbsolutePath().toString(); break; // -resourcePath (-rp)
                case "jarPath": case "jp": jarPath = Paths.get(value).toAbsolutePath().toString(); break; // -jarPath (-jp)
                case "fhirversion": case "v": fhirVersion = value; break;
                default: throw new IllegalArgumentException("Unknown flag: " + flag);
            }
        }
        if ((profilePath == null || profilePath.length() < 1) && (igLocation == null || igLocation.length() < 1)) {
            throw new IllegalArgumentException("Invalid argument: Either profile path or an ig path must be included. -pp=<paths to profile> OR -ig=<path to IG, a profile directory or a single profile file>");
        }
        if (resourcePath.length() < 1) {
            throw new IllegalArgumentException("Invalid argument: A resource path must be included. -rp=<path to resource(s) to validate>");
        }
        if(igLocation != null && profilePath != null){
            throw new IllegalArgumentException("Invalid argument: There may be AN -ig file OR a -profile, but not both");
        }
        if(igLocation != null && igLocation.length() > 0){
            profilesToUse = " -ig " + igLocation;
        }else if(profilePath != null && profilePath.length() > 0){
            profilesToUse = " " + profilePath;
        }
        executeValidation();
    }

    private void handleProfilePath(String value) {
        profilePath = profilePath + " -profile " + value;
    }

    private void executeValidation() {
        List<String> validationResults = new ArrayList<String >();
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        System.out.println(formatter.format(date));
        validationResults.add(String.format("Validation Results from : " + formatter.format(date)));
        for (File file : new File(resourcePath).listFiles()) {
            if (file.getName().endsWith(".json") || file.getName().endsWith(".xml")) {
                IBaseResource resource = IOUtils.readResource(file.getAbsolutePath(), getFhirContext());
                if (resource instanceof Bundle) {
                    throw new IllegalArgumentException("Invalid file in resource directory. Expected:Resource Found:Bundle. First run BundleToResources to convert " + file.getName()  + " to individual resources. ");
                }else {
                     validationResults.add(System.lineSeparator() + System.lineSeparator() +
                             file.getName() + System.lineSeparator() +
                             profilesToUse + System.lineSeparator());

                     validationResults.add(processResourceFile(file.getName()));
                }
            }
        }
        recordResults(validationResults);
    }

    private void recordResults(List<String> validationResults) {
        File resultsFile = new File(getOutputPath());
        if(!resultsFile.exists()){
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                System.out.println("File " + getOutputPath() + " could not be created. Check permissions.");
                e.printStackTrace();
            }
        }
        try {
            FileWriter resultsWriter = new FileWriter(resultsFile);
            for (String resultsString: validationResults) {
                resultsWriter.write(resultsString);
            }
            resultsWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String processResourceFile(String fileName) {
        String callJar = "java -jar " + jarPath + " " + resourcePath + File.separator + fileName + " -version " + fhirVersion + profilesToUse;// + " -txLog D:\\sandbox\\validator\\txlog.log";
        Process proc;
        StringBuffer sbResults = new StringBuffer();
        try {
            System.out.println(callJar);
            proc = Runtime.getRuntime().exec(callJar);
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sbResults.append(line + System.lineSeparator());
            }
            reader.close();
            final BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(proc.getErrorStream()));
            String errLine;
            while ((errLine = errReader.readLine()) != null) {
                sbResults.append(errLine + System.lineSeparator());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sbResults.toString();
    }

}
