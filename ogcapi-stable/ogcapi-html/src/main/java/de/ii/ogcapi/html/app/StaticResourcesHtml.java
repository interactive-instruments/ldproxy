package de.ii.ogcapi.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.web.domain.StaticResources;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class StaticResourcesHtml implements StaticResources {

  @Inject
  StaticResourcesHtml() {
  }

  @Override
  public String getResourcePath() {
    return "/assets";
  }

  @Override
  public String getUrlPath() {
    return "/ogcapi-html/assets";
  }
}
