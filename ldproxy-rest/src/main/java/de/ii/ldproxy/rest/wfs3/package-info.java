/**
 * @author zahnen
 */
@XmlSchema(
        namespace = "http://www.opengis.net/wfs/3.0",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix="wfs", namespaceURI="http://www.opengis.net/wfs/3.0")
        }
)
package de.ii.ldproxy.rest.wfs3;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;