package org.apereo.cas.logout;

import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.authentication.principal.AbstractWebApplicationService;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.AbstractRegisteredService;
import org.apereo.cas.services.LogoutType;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.util.http.HttpMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * @author Jerome Leleu
 * @since 4.0.0
 */
@RunWith(JUnit4.class)
public class LogoutManagerImplTests {

    private static final String ID = "id";

    private static final String URL = "http://www.github.com";

    private LogoutManagerImpl logoutManager;

    @Mock
    private TicketGrantingTicket tgt;

    private Map<String, Service> services;

    private AbstractWebApplicationService simpleWebApplicationServiceImpl;

    private AbstractRegisteredService registeredService;

    @Mock
    private ServicesManager servicesManager;

    @Mock
    private HttpClient client;

    public LogoutManagerImplTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setUp() {

        when(client.isValidEndPoint(any(String.class))).thenReturn(true);
        when(client.isValidEndPoint(any(URL.class))).thenReturn(true);
        when(client.sendMessageToEndPoint(any(HttpMessage.class))).thenReturn(true);
        this.logoutManager = new LogoutManagerImpl(new SamlCompliantLogoutMessageCreator());

        final DefaultSingleLogoutServiceMessageHandler handler = new DefaultSingleLogoutServiceMessageHandler();
        handler.setHttpClient(client);
        handler.setLogoutMessageBuilder(new SamlCompliantLogoutMessageCreator());
        handler.setServicesManager(servicesManager);
        handler.setSingleLogoutServiceLogoutUrlBuilder(new DefaultSingleLogoutServiceLogoutUrlBuilder());
        this.logoutManager.setSingleLogoutServiceMessageHandler(handler);

        this.services = new HashMap<>();
        this.simpleWebApplicationServiceImpl = RegisteredServiceTestUtils.getService(URL);
        this.services.put(ID, this.simpleWebApplicationServiceImpl);
        when(this.tgt.getServices()).thenReturn(this.services);

        this.registeredService = RegisteredServiceTestUtils.getRegisteredService(URL);
        when(servicesManager.findServiceBy(this.simpleWebApplicationServiceImpl)).thenReturn(this.registeredService);
    }

    @Test
    public void verifyServiceLogoutUrlIsUsed() throws Exception {
        this.registeredService.setLogoutUrl(new URL("https://www.apereo.org"));
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        final LogoutRequest logoutRequest = logoutRequests.iterator().next();
        assertEquals(logoutRequest.getLogoutUrl(), this.registeredService.getLogoutUrl());
    }

    @Test
    public void verifyLogoutDisabled() {
        this.logoutManager.setSingleLogoutCallbacksDisabled(true);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(0, logoutRequests.size());
    }

    @Test
    public void verifyLogoutAlreadyLoggedOut() {
        this.simpleWebApplicationServiceImpl.setLoggedOutAlready(true);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(0, logoutRequests.size());
    }

    @Test
    public void verifyLogoutTypeNotSet() {
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(1, logoutRequests.size());
        final LogoutRequest logoutRequest = logoutRequests.iterator().next();
        assertEquals(ID, logoutRequest.getTicketId());
        assertEquals(this.simpleWebApplicationServiceImpl, logoutRequest.getService());
        assertEquals(LogoutRequestStatus.SUCCESS, logoutRequest.getStatus());
    }

    @Test
    public void verifyLogoutTypeBack() {
        this.registeredService.setLogoutType(LogoutType.BACK_CHANNEL);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(1, logoutRequests.size());
        final LogoutRequest logoutRequest = logoutRequests.iterator().next();
        assertEquals(ID, logoutRequest.getTicketId());
        assertEquals(this.simpleWebApplicationServiceImpl, logoutRequest.getService());
        assertEquals(LogoutRequestStatus.SUCCESS, logoutRequest.getStatus());
    }

    @Test
    public void verifyLogoutTypeNone() {
        this.registeredService.setLogoutType(LogoutType.NONE);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(0, logoutRequests.size());
    }

    @Test
    public void verifyLogoutTypeNull() {
        this.registeredService.setLogoutType(null);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(1, logoutRequests.size());
        final LogoutRequest logoutRequest = logoutRequests.iterator().next();
        assertEquals(ID, logoutRequest.getTicketId());
    }

    @Test
    public void verifyLogoutTypeFront() {
        this.registeredService.setLogoutType(LogoutType.FRONT_CHANNEL);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(1, logoutRequests.size());
        final LogoutRequest logoutRequest = logoutRequests.iterator().next();
        assertEquals(ID, logoutRequest.getTicketId());
        assertEquals(this.simpleWebApplicationServiceImpl, logoutRequest.getService());
        assertEquals(LogoutRequestStatus.NOT_ATTEMPTED, logoutRequest.getStatus());
    }
    
    @Test
    public void verifyAsynchronousLogout() {
        this.registeredService.setLogoutType(LogoutType.BACK_CHANNEL);
        DefaultSingleLogoutServiceMessageHandler.class.cast(this.logoutManager.getSingleLogoutServiceMessageHandler()).
                setAsynchronous(true);
        final Collection<LogoutRequest> logoutRequests = this.logoutManager.performLogout(tgt);
        assertEquals(1, logoutRequests.size());
    }
}
