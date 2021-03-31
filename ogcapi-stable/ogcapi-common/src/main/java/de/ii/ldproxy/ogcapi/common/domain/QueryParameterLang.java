package de.ii.ldproxy.ogcapi.common.domain;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterLang implements OgcApiQueryParameter {

    private Schema schema = null;
    private final ExtensionRegistry extensionRegistry;

    public QueryParameterLang(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "lang";
    }

    @Override
    public String getDescription() {
        return "Select the language of the response. If no value is provided, " +
                "the standard HTTP rules apply, i.e., the accept-lang header will be used to determine the format.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        if (schema==null) {
            schema = new StringSchema()._enum(I18n.getLanguages().stream().map(lang -> lang.getLanguage()).collect(Collectors.toList()));
        }
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getUseLangParameter)
                .orElse(false);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CommonConfiguration.class;
    }
}
