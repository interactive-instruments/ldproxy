package de.ii.ldproxy.ogcapi.features.core.domain.processing;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureProcessChain {
    private static final String DAPA_PATH_ELEMENT = "dapa";
    private final List<FeatureProcess> processes;

    public FeatureProcessChain(List<FeatureProcess> processes) {
        this.processes = processes;
    }

    @Value.Derived
    @Value.Auxiliary
    public List<FeatureProcess> asList() { return processes; }

    /**
     *
     * @return the path after {@code /collections/{collectionId}}
     */
    @Value.Derived
    @Value.Auxiliary
    public String getSubSubPath() {
        return "/"+DAPA_PATH_ELEMENT+"/" + String.join(":", processes.stream().map(process -> process.getName()).collect(Collectors.toList()));
    }

    /**
     *
     * @param processNames names of the process to look for, "*" is a wildcard
     * @return {@code true}, if the process chain includes a process with any of the names in {@code processName}
     */
    @Value.Derived
    @Value.Auxiliary
    public boolean includes(String... processNames) {
        for (String name : processNames) {
            if (processes.stream().anyMatch(process -> process.getName().equals(name) || name.equals("*")))
                return true;
        }
        return false;
    }

    public String getOperationSummary() {
        return processes.get(processes.size()-1).getSummary();
    }

    public Optional<String> getOperationDescription() {
        return processes.get(processes.size()-1).getDescription();
    }

    public Optional<String> getResponseDescription() {
        return Optional.empty();
    }
}
