package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterLang implements OgcApiQueryParameter {

    @Requires
    OgcApiExtensionRegistry extensionRegistry;

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
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET;
    }

    private Schema schema = null;

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        if (schema==null) {
            schema = new StringSchema()._enum(I18n.getLanguages().stream().map(lang -> lang.getLanguage()).collect(Collectors.toList()));
        }
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCommonConfiguration.class) &&
                apiData.getExtension(OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getUseLangParameter)
                .orElse(false);
    }

}
