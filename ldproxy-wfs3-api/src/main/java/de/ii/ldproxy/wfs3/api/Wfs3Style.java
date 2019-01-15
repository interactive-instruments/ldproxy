package de.ii.ldproxy.wfs3.api;

public class Wfs3Style {

    private final String styleId;
    private final String styleUrl;

    public Wfs3Style(String styleId, String styleUrl){
        this.styleId = styleId;
        this.styleUrl = styleUrl;
    }

    public String getStyleId(){
        return styleId;
    }

    public String getStyleUrl(){
        return styleUrl;
    }
}
