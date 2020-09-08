package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServiceListingProvider;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ApiCatalogProvider implements ServiceListingProvider {

    protected final BundleContext bundleContext;
    protected final XtraPlatform xtraPlatform;
    protected final I18n i18n;
    protected final EntityDataDefaultsStore defaultsStore;
    protected final ExtensionRegistry extensionRegistry;

    public ApiCatalogProvider(@Context BundleContext bundleContext,
                              @Requires XtraPlatform xtraPlatform,
                              @Requires I18n i18n,
                              @Requires EntityDataDefaultsStore defaultsStore,
                              @Requires ExtensionRegistry extensionRegistry) {
        this.bundleContext = bundleContext;
        this.xtraPlatform = xtraPlatform;
        this.i18n = i18n;
        this.defaultsStore = defaultsStore;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Response getServiceListing(List<ServiceData> apis, URI uri) {
        try {
            return getServiceListing(apis, uri, Optional.of(Locale.ENGLISH));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not generate service overview.", e);
        }
    }

    public abstract Response getServiceListing(List<ServiceData> services, URI uri, Optional<Locale> language) throws URISyntaxException;

    @Override
    public Response getStaticAsset(String path) {

        throw new RuntimeException(String.format("Static assets not supported for class '%s'. File requested at path '%s'.", this.getClass().getSimpleName(), path));
    }

    private Optional<URI> getExternalUri() {
        return Optional.ofNullable(xtraPlatform.getServicesUri());
    }

    private void customizeUri(final URICustomizer uriCustomizer) {
        if (getExternalUri().isPresent()) {
            uriCustomizer.setScheme(getExternalUri().get()
                                                    .getScheme());
            uriCustomizer.replaceInPath("/rest/services", getExternalUri().get()
                                                                          .getPath());
            uriCustomizer.ensureNoTrailingSlash();
        }
    }

    private String getStaticUrlPrefix(final URICustomizer uriCustomizer) {
        if (getExternalUri().isPresent()) {
            return uriCustomizer.copy()
                                .cutPathAfterSegments("rest", "services")
                                .replaceInPath("/rest/services", getExternalUri().get()
                                                                                 .getPath())
                                .ensureLastPathSegment("___static___")
                                .ensureTrailingSlash()
                                .getPath();
        }

        return "";
    }

    private FoundationConfiguration getConfig() {
        //TODO: encapsulate in entities/defaults layer
        EntityDataBuilder<?> builder = defaultsStore.getBuilder(Identifier.from(EntityDataDefaultsStore.EVENT_TYPE, Service.TYPE, OgcApiDataV2.SERVICE_TYPE.toLowerCase()));
        if (builder instanceof ImmutableOgcApiDataV2.Builder) {
            ImmutableOgcApiDataV2 defaults = ((ImmutableOgcApiDataV2.Builder) builder).id("NOT_SET")
                                                                                      .build();
            return defaults.getExtension(FoundationConfiguration.class)
                           .orElse(new ImmutableFoundationConfiguration.Builder().build());
        }
        return new ImmutableFoundationConfiguration.Builder().build();
    }

    protected URI getApiUrl(URI uri, String apiId, Optional<Integer> apiVersion) throws URISyntaxException {
        return apiVersion.isPresent() ?
            new URICustomizer(uri).clearParameters().ensureLastPathSegments(apiId, "v"+apiVersion.get()).ensureNoTrailingSlash().build() :
            new URICustomizer(uri).clearParameters().ensureLastPathSegment(apiId).ensureNoTrailingSlash().build();
    }

    protected ApiCatalog getCatalog(List<ServiceData> services, URI uri, Optional<Locale> language) throws URISyntaxException {
        final DefaultLinksGenerator linksGenerator = new DefaultLinksGenerator();
        URICustomizer uriCustomizer = new URICustomizer(uri);
        String urlPrefix = getStaticUrlPrefix(uriCustomizer);
        customizeUri(uriCustomizer);
        try {
            uri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            // ignore
        }

        // TODO we do not have the additional information
        ApiMediaType mediaType = new ImmutableApiMediaType.Builder()
                .type(getMediaType())
                .build();
        // TODO we do not have this information
        List<ApiMediaType> alternateMediaTypes = ImmutableList.<ApiMediaType>of();

        FoundationConfiguration config = getConfig();

        URI finalUri = uri;
        ImmutableApiCatalog.Builder builder = new ImmutableApiCatalog.Builder()
                .title(Objects.requireNonNullElse(config.getApiCatalogLabel(), i18n.get("rootTitle", language)))
                .description(Objects.requireNonNullElse(config.getApiCatalogDescription(), i18n.get("rootDescription", language)))
                .catalogUri(new URICustomizer(finalUri).clearParameters().ensureNoTrailingSlash().build())
                .urlPrefix(urlPrefix)
                // TODO skip links for now until we can properly create self/alternate links
                // .links(linksGenerator.generateLinks(uriCustomizer, mediaType, alternateMediaTypes, i18n, language))
                .apis(services.stream()
                              .sorted(Comparator.comparing(ServiceData::getLabel))
                              .map(api -> {
                                  try {
                                      return new ImmutableApiCatalogEntry.Builder()
                                              .id(api.getId())
                                              .title(api.getLabel())
                                              .description(api.getDescription())
                                              .landingPageUri(getApiUrl(finalUri, api.getId(), api.getApiVersion()))
                                              .build();
                                  } catch (URISyntaxException e) {
                                      throw new RuntimeException(String.format("Could not create landing page URI for API '%s'.", api.getId()), e);
                                  }
                              })
                              .collect(Collectors.toList()));

        for (ApiCatalogExtension extension : extensionRegistry.getExtensionsForType(ApiCatalogExtension.class)) {
            builder = extension.process(builder,
                                        uriCustomizer.copy(),
                                        mediaType,
                                        alternateMediaTypes,
                                        language);
        }

        return builder.build();
    }
}
