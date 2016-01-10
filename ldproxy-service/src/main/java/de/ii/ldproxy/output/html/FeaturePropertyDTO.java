package de.ii.ldproxy.output.html;

import com.github.mustachejava.util.DecoratedCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class FeaturePropertyDTO {
    public String itemType;
    public String itemProp;
    public String name;
    public String value;
    protected List<FeaturePropertyDTO> childList;
    public FeaturePropertyDTO parent;

    public FeaturePropertyDTO() {
        this.childList = new ArrayList<>();
    }

    public String microdata() {
        String microdata = "";

        if (itemProp != null && !itemProp.isEmpty()) {
            microdata += "itemprop=\"" + itemProp + "\"";
        }
        if (itemType != null && !itemType.isEmpty()) {
            microdata += " itemscope itemtype=\"" + itemType + "\"";
        }

        return microdata;
    }

    public DecoratedCollection<FeaturePropertyDTO> children() {
        return childList.size() > 0 ? new DecoratedCollection<FeaturePropertyDTO>(childList) : null;
    }

    public void addChild(FeaturePropertyDTO child) {
        childList.add(child);
        child.parent = this;
    }
}
