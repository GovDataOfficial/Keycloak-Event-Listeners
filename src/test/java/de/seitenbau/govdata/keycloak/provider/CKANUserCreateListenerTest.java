package de.seitenbau.govdata.keycloak.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.seitenbau.govdata.keycloak.model.KeycloakUser;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CKANUserCreateListenerTest {
  private static final String REALM = "testRealm";
  private static final String AUTH_USER = "user";
  private static final String AUTH_PASSWORD = "password";
  private static final String SERVICE_URL = "http://example.com";
  private HttpClient.Builder httpClientBuilderMock;
  private ObjectMapper objectMapperMock;
  private CKANUserCreateListener sut;

  @BeforeEach
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    httpClientBuilderMock = mock(HttpClient.Builder.class);
    objectMapperMock = mock(ObjectMapper.class);
    sut = new CKANUserCreateListener(REALM, SERVICE_URL, AUTH_USER, AUTH_PASSWORD, httpClientBuilderMock, objectMapperMock);
  }


  @Test
  public void onEvent_UserCreatedInCKAN()
      throws IOException, InterruptedException, URISyntaxException
  {
    String newUser = "testUser";
    AdminEvent event = new AdminEvent();
    event.setOperationType(OperationType.CREATE);
    event.setResourceType(ResourceType.USER);
    event.setRealmName(REALM);
    event.setRepresentation("{\"username\":\"" + newUser + "\"}");

    KeycloakUser mockUser = new KeycloakUser();
    mockUser.setUsername(newUser);

    when(objectMapperMock.readValue(event.getRepresentation(), KeycloakUser.class)).thenReturn(mockUser);

    HttpClient httpClientMock = mock(HttpClient.class);
    when(httpClientBuilderMock.build()).thenReturn(httpClientMock);
    HttpResponse<Object> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(newUser);
    when(httpClientMock.send(any(), any())).thenReturn(mockResponse);

    sut.onEvent(event, true);

    String auth = AUTH_USER + ":" + AUTH_PASSWORD;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    HttpRequest expectedRequest = HttpRequest.newBuilder()
        .uri(new URI(SERVICE_URL + "/govdata-data/find-or-create-user?username=" + newUser))
        .header("Authorization", "Basic " + encodedAuth)
        .GET()
        .build();
    verify(httpClientMock).send(eq(expectedRequest), any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void onEvent_InvalidUserRepresentation() throws JsonProcessingException {
    AdminEvent event = new AdminEvent();
    event.setOperationType(OperationType.CREATE);
    event.setResourceType(ResourceType.USER);
    event.setRealmName(REALM);
    event.setRepresentation("invalid json");

    when(objectMapperMock.readValue(event.getRepresentation(), KeycloakUser.class)).thenThrow(JsonProcessingException.class);

    sut.onEvent(event, true);

    verify(objectMapperMock).readValue(event.getRepresentation(), KeycloakUser.class);
    verifyNoInteractions(httpClientBuilderMock);
  }

  @ParameterizedTest
  @EnumSource(value = OperationType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CREATE"})
  public void onEvent_OperationTypeNotCreate(OperationType operationType) {
    AdminEvent event = new AdminEvent();
    event.setOperationType(operationType);
    event.setResourceType(ResourceType.USER);
    event.setRealmName(REALM);

    sut.onEvent(event, true);

    verifyNoInteractions(objectMapperMock);
    verifyNoInteractions(httpClientBuilderMock);
  }

  @ParameterizedTest
  @EnumSource(value = ResourceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"USER"})
  public void onEvent_ResourceTypeNotUser(ResourceType resourceType) {
    AdminEvent event = new AdminEvent();
    event.setOperationType(OperationType.CREATE);
    event.setResourceType(resourceType);
    event.setRealmName(REALM);

    sut.onEvent(event, true);

    verifyNoInteractions(objectMapperMock);
    verifyNoInteractions(httpClientBuilderMock);
  }

}