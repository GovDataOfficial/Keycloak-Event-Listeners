package de.seitenbau.govdata.keycloak.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.seitenbau.govdata.keycloak.model.KeycloakUser;

public class CKANUserCreateListener implements EventListenerProvider
{
  private static final Logger logger = Logger.getLogger(CKANUserCreateListener.class);
  private HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
  private ObjectMapper objectMapper = new ObjectMapper();
  private final String realmName;
  private final String dataServiceUrl;
  private final String dataServiceUser;
  private final String dataServicePassword;

  public CKANUserCreateListener(
      String realmName,
      String dataServiceUrl,
      String dataServiceUser,
      String dataServicePassword)
  {
    this.realmName = realmName;
    this.dataServiceUrl = dataServiceUrl;
    this.dataServiceUser = dataServiceUser;
    this.dataServicePassword = dataServicePassword;
  }

  public CKANUserCreateListener(
      String realmName,
      String dataServiceUrl,
      String dataServiceUser,
      String dataServicePassword,
      HttpClient.Builder httpClientBuilder,
      ObjectMapper objectMapper)
  {
    this.realmName = realmName;
    this.dataServiceUrl = dataServiceUrl;
    this.dataServiceUser = dataServiceUser;
    this.dataServicePassword = dataServicePassword;
    this.httpClientBuilder = httpClientBuilder;
    this.objectMapper = objectMapper;
  }

  @Override
  public void onEvent(Event event)
  {
  }

  private void createUserInCKAN(String userId)
  {
    URI uri;
    try
    {
      String baseUrl = dataServiceUrl + "/govdata-data/find-or-create-user";
      uri = new URI(baseUrl + "?username=" + userId);
    }
    catch (URISyntaxException e)
    {
      logger.error("Error creating URI", e);
      return;
    }

    String auth = dataServiceUser + ":" + dataServicePassword;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("Authorization", "Basic " + encodedAuth)
        .GET()
        .build();

    try
    {
      HttpClient client = httpClientBuilder.build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (HttpStatus.SC_OK != response.statusCode())
      {
        logger.error("Failed to find or create user in CKAN. CKAN responded with HTTP " + response.statusCode() + ": " + response.body());
      } else {
        logger.info("User \"" + response.body() + "\" either created or found in CKAN");
      }
    }
    catch (IOException | InterruptedException e)
    {
      logger.error("Error sending user creation request to CKAN: ", e);
    }
  }

  @Override
  public void close()
  {
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation)
  {
    if (OperationType.CREATE.equals(event.getOperationType()) &&
        ResourceType.USER.equals(event.getResourceType()) &&
        event.getRealmName().equals(realmName)
    )
    {
      logger.info("User creation event triggered in " + realmName + " realm");
      try
      {
        KeycloakUser user = objectMapper.readValue(event.getRepresentation(), KeycloakUser.class);
        createUserInCKAN(user.getUsername());
      }
      catch (JsonProcessingException e)
      {
        logger.error("Error parsing user representation", e);
      }
    }

  }
}
