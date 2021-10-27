package org.opencds.cqf.tooling.demo;

import org.testng.annotations.Test;

public class MeasureUrlPrinterTest {
    
    @Test
    public void printUrls() {
        printUrl((MeasureLike)new org.hl7.fhir.dstu3.model.Measure().setUrl("dstu3.com"));
        printUrl((MeasureLike)new org.hl7.fhir.r4.model.Measure().setUrl("r4.com"));
    }

    public void printUrl(MeasureLike measureLike) {
        System.out.println(measureLike.getUrl());
    }
}
