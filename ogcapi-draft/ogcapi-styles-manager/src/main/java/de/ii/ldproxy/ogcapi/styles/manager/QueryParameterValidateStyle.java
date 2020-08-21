package de.ii.ldproxy.ogcapi.styles.manager;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.StylesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Optional;

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
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
               ((method== HttpMethods.PUT && definitionPath.equals("/styles/{styleId}")) ||
                (method== HttpMethods.POST && definitionPath.equals("/styles")));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> extension = apiData.getExtension(StylesConfiguration.class);

        return extension
                .filter(StylesConfiguration::isEnabled)
                .filter(StylesConfiguration::getManagerEnabled)
                .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

}
