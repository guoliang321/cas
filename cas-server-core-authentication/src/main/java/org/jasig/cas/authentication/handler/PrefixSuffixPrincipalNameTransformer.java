package org.jasig.cas.authentication.handler;

/**
 * Transform the user id by adding a prefix or suffix.
 *
 * @author Howard Gilbert
 * @author Scott Battaglia

 * @since 3.3.6
 */

public final class PrefixSuffixPrincipalNameTransformer implements PrincipalNameTransformer {

    private String prefix;

    private String suffix;

    /**
     * Instantiates a new Prefix suffix principal name transformer.
     */
    public PrefixSuffixPrincipalNameTransformer() {
        this.prefix = null;
        this.suffix = null;
    }

    /**
     * Instantiates a new Prefix suffix principal name transformer.
     *
     * @param prefix the prefix
     * @param suffix the suffix
     */
    public PrefixSuffixPrincipalNameTransformer(final String prefix, final String suffix) {
        setPrefix(prefix);
        setSuffix(suffix);
    }

    @Override
    public String transform(final String formUserId) {
        final StringBuilder stringBuilder = new StringBuilder();

        if (this.prefix != null) {
            stringBuilder.append(this.prefix);
        }

        stringBuilder.append(formUserId);

        if (this.suffix != null) {
            stringBuilder.append(this.suffix);
        }

        return stringBuilder.toString();
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(final String suffix) {
        this.suffix = suffix;
    }
}
