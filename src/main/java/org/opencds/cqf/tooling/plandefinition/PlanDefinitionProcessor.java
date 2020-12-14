package org.opencds.cqf.tooling.plandefinition;

public class PlanDefinitionProcessor {
    public static final String ResourcePrefix = "plandefinition-";
    public static String getId(String baseId) {
        return ResourcePrefix + baseId;
    }
}
