import { customTheme as xtraplatformTheme } from "xtraplatform-manager/src/theme";
import { deepMerge } from "grommet/utils";
import { css } from 'styled-components';

export const customTheme = deepMerge(xtraplatformTheme, {
    global: {
        colors: {
            active: "#417baa",
            brand: "#326499",
            menu: "light-6",
            'light-6': "#9296a0",
            'dark-6': "#383f51"
        }
    }
});
