import { launch, app } from "@xtraplatform/manager";
import { Services, Codelists } from "@xtraplatform/services";
import Common from "@ogcapi/common";
import { createFeature } from "feature-u";

const Ldproxy = createFeature({
  name: "app-ldproxy",
  fassets: {
    // provided resources
    define: {
      [app("ldproxy")]: {
        name: "ldproxy",
        defaultTheme: "default",
      },
    },
  },
});

launch([Services, Codelists, Common, Ldproxy]);
