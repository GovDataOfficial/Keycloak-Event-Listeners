# GovData Keycloak User Create Listener

A custom keycloak provider that implements an event listener which calls the CKAN API to create a user in CKAN for each 
newly created user in keycloak.

## Getting Started
* Build the project
    * `mvn clean install`
  

* Copy the jar file to the provider folder in the keycloak installation directory
    * `cp target/keycloak-event-listeners-<version>.jar /path/to/keycloak/providers/`


* (Re-)Start the keycloak server


* Enable the event listener
    * Open the realm settings of the realm where the users are created
    * Events -> Event listeners -> Choose `ckan-user-create-listener` from the dropdown to enable it

## Helpful Links
* For more information on custom providers see the [keycloak server configuration guide](https://www.keycloak.org/server/configuration-provider)
