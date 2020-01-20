package de.ii.ldproxy.target.html;

import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertyTransformations;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

@Value.Immutable
public abstract class HtmlPropertyTransformations implements FeaturePropertyTransformations<FeaturePropertyDTO> {

    @Override
    public String getValue(FeaturePropertyDTO wrapper) {
        return wrapper.value;
    }

    @Override
    public Optional<FeaturePropertyDTO> transform(FeaturePropertyDTO wrapper, FeatureProperty transformedSchema,
                                                  String transformedValue) {
        if (Objects.isNull(transformedSchema)) {
            return Optional.empty();
        }

        wrapper.name = transformedSchema.getName();
        wrapper.value = transformedValue;


        if (Objects.isNull(wrapper.value)) {
            return Optional.of(wrapper);
        }

        if (isHtml(wrapper.value)) {
            wrapper.isHtml = true;
        } else if (isImageUrl(wrapper.value)) {
            wrapper.isImg = true;
        } else if (isUrl(wrapper.value)) {
            wrapper.isUrl = true;
        }

        return Optional.of(wrapper);
    }

    private boolean isHtml(String value) {
        return value.startsWith("<") && (value.endsWith(">") || value.endsWith(">\n")) && value.contains("</");
    }

    private boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean isImageUrl(String value) {
        return isUrl(value) && (value.toLowerCase()
                                     .endsWith(".png") || value.toLowerCase()
                                                               .endsWith(".jpg") || value.toLowerCase()
                                                                                         .endsWith(".gif"));
    }
}
