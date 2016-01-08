package de.ii.ldproxy.output.html;

/**
 * @author zahnen
 */
public class NavigationDTO {
    public String label;
    public String url;

    public NavigationDTO(String label) {
        this.label = label;
    }

    public NavigationDTO(String label, String url) {
        this.label = label;
        this.url = url;
    }
}
