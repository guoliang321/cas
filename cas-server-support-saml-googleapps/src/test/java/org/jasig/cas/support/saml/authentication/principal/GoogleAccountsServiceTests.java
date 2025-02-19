package org.jasig.cas.support.saml.authentication.principal;

import org.jasig.cas.authentication.principal.DefaultResponse;
import org.jasig.cas.authentication.principal.Response;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.DefaultRegisteredServiceUsernameProvider;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.support.saml.AbstractOpenSamlTests;
import org.jasig.cas.support.saml.SamlProtocolConstants;
import org.jasig.cas.util.CompressionUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Scott Battaglia
 * @since 3.1
 */
public class GoogleAccountsServiceTests extends AbstractOpenSamlTests {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private GoogleAccountsServiceFactory factory;

    private GoogleAccountsService googleAccountsService;

    public GoogleAccountsService getGoogleAccountsService() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String samlRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              + "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
              + "ID=\"5545454455\" Version=\"2.0\" IssueInstant=\"Value\" "
              + "ProtocolBinding=\"urn:oasis:names.tc:SAML:2.0:bindings:HTTP-Redirect\" "
              + "ProviderName=\"https://localhost:8443/myRutgers\" AssertionConsumerServiceURL=\"https://localhost:8443/myRutgers\"/>";
        request.setParameter(SamlProtocolConstants.PARAMETER_SAML_REQUEST, encodeMessage(samlRequest));
        request.setParameter(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE, "RelayStateAddedHere");

        final RegisteredService regSvc = mock(RegisteredService.class);
        when(regSvc.getUsernameAttributeProvider()).thenReturn(new DefaultRegisteredServiceUsernameProvider());
        
        final ServicesManager servicesManager = mock(ServicesManager.class);
        when(servicesManager.findServiceBy(any(Service.class))).thenReturn(regSvc);
        
        return factory.createService(request);
    }

    @Before
    public void setUp() throws Exception {
        this.googleAccountsService = getGoogleAccountsService();
        this.googleAccountsService.setPrincipal(org.jasig.cas.authentication.TestUtils.getPrincipal());
    }

    @Test
    public void verifyResponse() {
        final Response resp = this.googleAccountsService.getResponse("ticketId");
        assertEquals(resp.getResponseType(), DefaultResponse.ResponseType.POST);
        assertTrue(resp.getAttributes().containsKey(SamlProtocolConstants.PARAMETER_SAML_RESPONSE));
        assertTrue(resp.getAttributes().containsKey(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE));
    }

    private static String encodeMessage(final String xmlString) throws IOException {
        return CompressionUtils.deflate(xmlString);
    }
}
