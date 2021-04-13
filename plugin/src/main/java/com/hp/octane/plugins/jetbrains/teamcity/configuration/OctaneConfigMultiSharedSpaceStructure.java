package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "configs")
public class OctaneConfigMultiSharedSpaceStructure {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName ="octane-config" )
    private List<OctaneConfigStructure> multiConfigStructure = new ArrayList<>();


    public List<OctaneConfigStructure> getMultiConfigStructure() {
        return multiConfigStructure;
    }

    public void setMultiConfigStructure(List<OctaneConfigStructure> multiConfigStructure) {
        this.multiConfigStructure = multiConfigStructure;
    }
}
