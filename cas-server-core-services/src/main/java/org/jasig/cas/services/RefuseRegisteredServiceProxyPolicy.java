package org.jasig.cas.services;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URL;

/**
 * A proxy policy that disallows proxying.
 * @author Misagh Moayyed
 * @since 4.1.0
 */
public final class RefuseRegisteredServiceProxyPolicy implements RegisteredServiceProxyPolicy {

    private static final long serialVersionUID = -5718445151129901484L;

    @Override
    public boolean isAllowedToProxy() {
        return false;
    }

    @Override
    public boolean isAllowedProxyCallbackUrl(final URL pgtUrl) {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        return o instanceof RefuseRegisteredServiceProxyPolicy;
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder bldr = new HashCodeBuilder(13, 133);
        return bldr.appendSuper(super.hashCode()).toHashCode();
    }
}
