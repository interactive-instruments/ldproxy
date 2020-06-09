package de.ii.ldproxy.wfs3.styles.manager;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.styles.StyleFormatExtension;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterValidateStyle implements OgcApiQueryParameter {

    private Schema schema = new StringSchema()._enum(ImmutableList.of("yes","no","only"))._default("no");

    @Override
    public String getId() {
        return "validateStyle";
    }

    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getDescription() {
        return "'yes' creates or updates a style after successful validation and returns 400," +
                "if validation fails. ’no' creates or updates the style without validation. 'only' just " +
                "validates the style without creating a new style or updating an existing style " +
                "and returns 400, if validation fails, otherwise 204.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
               ((method==OgcApiContext.HttpMethods.PUT && definitionPath.equals("/styles/{styleId}")) ||
                (method==OgcApiContext.HttpMethods.POST && definitionPath.equals("/styles")));
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> extension = getExtensionConfiguration(apiData, StylesConfiguration.class);

        return extension
                .filter(StylesConfiguration::getEnabled)
                .filter(StylesConfiguration::getManagerEnabled)
                .isPresent();
    }

}
