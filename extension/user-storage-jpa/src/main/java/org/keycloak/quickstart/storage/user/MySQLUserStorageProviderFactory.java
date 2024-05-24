/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.quickstart.storage.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.quickstart.storage.user.enums.ConfigProperties;
import org.keycloak.quickstart.storage.user.enums.UserAttributes;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MySQLUserStorageProviderFactory implements UserStorageProviderFactory<MySQLUserStorageProvider>, ImportSynchronization {
    public static final String PROVIDER_ID = "my-sql-user-storage-jpa";

    private static final Logger logger = Logger.getLogger(MySQLUserStorageProviderFactory.class);

    private Timestamp lastSync;

    private int pageSize = 0;

    private int pageNumber = 0;

    private int userCount = 0;

    @Override
    public MySQLUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new MySQLUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "JPA MySQL User Storage Provider";
    }

    @Override
    public void close() {
        logger.info("<<<<<< Closing factory");
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        ProviderConfigProperty property;
        List<String> options = new ArrayList<>();
        options.add("50");
        options.add("100");
        options.add("150");
        options.add("200");

        property = new ProviderConfigProperty();
        property.setName(String.valueOf(ConfigProperties.NUMBER_OF_USERS_TO_SYNC));
        property.setLabel("Number of users to sync");
        property.setHelpText("Synchronize users by chunk");
        property.setDefaultValue("100");
        property.setOptions(options);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        configProperties.add(property);

        return configProperties;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        String numberOfUsersToSync = model.getConfig().getFirst(String.valueOf(ConfigProperties.NUMBER_OF_USERS_TO_SYNC));

        if (numberOfUsersToSync == null || numberOfUsersToSync.isEmpty() || numberOfUsersToSync.isBlank())
            throw new ComponentValidationException("Configurations not properly set. Please verify.");

        try {
            this.pageSize = Integer.parseInt(numberOfUsersToSync);
        } catch (NumberFormatException e) {
            logger.info("Error: Number of users to sync is not a valid integer.");
        }
    }

    // called every n seconds, such that n is the value of the "Full sync period" settings
    @Override
    public SynchronizationResult sync(KeycloakSessionFactory keycloakSessionFactory, String realmId, UserStorageProviderModel userStorageProviderModel) {
        SynchronizationResult result = new SynchronizationResult();

        KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, session -> {
            EntityManager externalEntityManager = session.getProvider(JpaConnectionProvider.class, "custom-user-store").getEntityManager();

            var query = externalEntityManager.createNamedQuery("getAllUsers", UserEntity.class)
                    .setFirstResult(0)
                    .setMaxResults(pageSize);

            var count = externalEntityManager.createNamedQuery("getUserCount")
                    .getSingleResult();
            userCount = ((Number)count).intValue();

            iterateByChunks(keycloakSessionFactory, realmId, result, query);
        });

        return result;
    }

    // called every n seconds, such that n is the value of the "Changed users sync period" settings
    @Override
    public SynchronizationResult syncSince(Date date, KeycloakSessionFactory keycloakSessionFactory, String realmId, UserStorageProviderModel userStorageProviderModel) {
        SynchronizationResult result = new SynchronizationResult();

        try {
            KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, session -> {
                EntityManager externalEntityManager = session.getProvider(JpaConnectionProvider.class, "custom-user-store").getEntityManager();

                lastSync = externalEntityManager
                        .createNamedQuery("getLastSyncDate", Timestamp.class)
                        .getSingleResult();

                var query = externalEntityManager.createNamedQuery("getUsersChangedSince", UserEntity.class)
                        .setParameter("lastSync", lastSync)
                        .setFirstResult(0)
                        .setMaxResults(pageSize);

                iterateByChunks(keycloakSessionFactory, realmId, result, query);
            });
        } catch (Exception e) {
            logger.error("Error occurred during synchronization", e);
            throw e;
        }

        return result;
    }

    private void iterateByChunks(KeycloakSessionFactory keycloakSessionFactory, String realmId, SynchronizationResult result, TypedQuery<UserEntity> query) {
        List<UserEntity> users = query.getResultList();

        while(!users.isEmpty()) {
            modifyUsers(keycloakSessionFactory, realmId, users, result);

            ++pageNumber;

            query.setFirstResult(pageNumber * pageSize);
            users = query.getResultList();
        }
        pageNumber = 0;
    }

    private void modifyUsers(KeycloakSessionFactory keycloakSessionFactory, String realmId, List<UserEntity> users, SynchronizationResult result) {
        for (UserEntity userEntity : users) {
            KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, session -> {
                UserModel userModel = session.users().getUserByUsername(session.realms().getRealm(realmId), userEntity.getUsername());

                if (userModel == null) {
                    userModel = session.users().addUser(session.realms().getRealm(realmId), userEntity.getUsername());
                    userModel.setCreatedTimestamp(userEntity.getCreatedAt().getTime());
                    result.increaseAdded();
                } else result.increaseUpdated();

                userModel.setUsername(userEntity.getUsername());
                userModel.setEmail(userEntity.getUsername());
                userModel.setFirstName(userEntity.getFirstName());
                userModel.setLastName(userEntity.getLastName());
                userModel.setEmailVerified(true);

                if (userEntity.getStatus() != null && !userEntity.getStatus().isEmpty() && !userEntity.getStatus().isBlank())
                    userModel.setSingleAttribute(String.valueOf(UserAttributes.STATUS), userEntity.getStatus());
                if (userEntity.getMobilePhone() != null && !userEntity.getMobilePhone().isEmpty() && !userEntity.getMobilePhone().isBlank())
                    userModel.setSingleAttribute(String.valueOf(UserAttributes.MOBILE_PHONE), userEntity.getMobilePhone());
                if (userEntity.getOfficePhone() != null && !userEntity.getOfficePhone().isEmpty() && !userEntity.getOfficePhone().isBlank())
                    userModel.setSingleAttribute(String.valueOf(UserAttributes.OFFICE_PHONE), userEntity.getOfficePhone());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String formattedUtcDateTime = Instant.now().atZone(ZoneOffset.UTC).format(formatter);
                Timestamp timestampUtc = Timestamp.valueOf(formattedUtcDateTime);
                userEntity.setLastSyncDate(timestampUtc);
            });
        }
    }
}
