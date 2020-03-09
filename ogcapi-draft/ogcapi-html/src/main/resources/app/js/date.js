/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
if ($('#timeInterval').length ) {
    var fromSet = (window._ldproxy.time.start != 0);
    var toSet = (dateFromMilliseconds(window._ldproxy.time.end) != dateFromMilliseconds(new Date()));
    if (fromSet && toSet) {
        document.getElementById("timeInterval").innerHTML = dateFromMilliseconds(window._ldproxy.time.start).concat(" - ").concat(dateFromMilliseconds(window._ldproxy.time.end));
    } else if (!fromSet && toSet) {
        document.getElementById("timeInterval").innerHTML = ".. - ".concat(dateFromMilliseconds(window._ldproxy.time.end));
    } else if (fromSet && !toSet) {
        document.getElementById("timeInterval").innerHTML = dateFromMilliseconds(window._ldproxy.time.start).concat(" - ..");
    } else {
        document.getElementById("timeInterval").innerHTML = "-";
    }
}

function dateFromMilliseconds(timestamp) {
    var date = new Date(timestamp);
    return date.toLocaleDateString();
}
