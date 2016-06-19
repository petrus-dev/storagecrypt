package fr.petrus.lib.core.rest.models.hubic;

/**
 * This class holds the results returned by some HubiC API calls.
 *
 * <p>It is filled with the JSON response of the API call.
 *
 * @author Pierre Sagne
 * @since 19.06.2016
 */
public class HubicAccountUsage {
    public Long quota;
    public Long used;

    public HubicAccountUsage() {
        quota = null;
        used = null;
    }
}
