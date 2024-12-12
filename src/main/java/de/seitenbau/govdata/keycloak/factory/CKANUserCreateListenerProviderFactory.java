package de.seitenbau.govdata.keycloak.factory;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import de.seitenbau.govdata.keycloak.provider.CKANUserCreateListener;

public class CKANUserCreateListenerProviderFactory implements EventListenerProviderFactory
{
  private String realmName;
  private String dataServiceUrl;
  private String dataServiceUser;
  private String dataServicePassword;

  @Override
  public EventListenerProvider create(KeycloakSession session)
  {
    return new CKANUserCreateListener(realmName, dataServiceUrl, dataServiceUser, dataServicePassword);
  }

  @Override
  public void init(Config.Scope config)
  {
    realmName = config.get("realm-name");
    dataServiceUrl = config.get("data-service-url");
    dataServiceUser = config.get("data-service-user");
    dataServicePassword = config.get("data-service-password");
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory)
  {
  }

  @Override
  public void close()
  {
  }

  @Override
  public String getId()
  {
    return "ckan-user-create-listener";
  }
}
