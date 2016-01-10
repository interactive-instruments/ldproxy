package de.ii.ldproxy.output.html;

/**
 * @author zahnen
 */
public class MicrodataPropertyMapping implements MicrodataMapping {

    private boolean enabled;
    private String name;
    private MICRODATA_TYPE type;
    private boolean showInCollection;
    private String itemType;
    private String itemProp;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MICRODATA_TYPE getType() {
        return type;
    }

    public void setType(MICRODATA_TYPE type) {
        this.type = type;
    }

    @Override
    public boolean isShowInCollection() {
        return showInCollection;
    }

    public void setShowInCollection(boolean showInCollection) {
        this.showInCollection = showInCollection;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public String getItemProp() {
        return itemProp;
    }

    public void setItemProp(String itemProp) {
        this.itemProp = itemProp;
    }
}
