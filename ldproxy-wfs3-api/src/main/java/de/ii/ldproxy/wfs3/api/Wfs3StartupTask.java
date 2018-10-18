package de.ii.ldproxy.wfs3.api;

/**
 * @author zahnen
 */
public interface Wfs3StartupTask extends Wfs3Extension {
    Runnable getTask(Wfs3ServiceData wfs3ServiceData);
}
