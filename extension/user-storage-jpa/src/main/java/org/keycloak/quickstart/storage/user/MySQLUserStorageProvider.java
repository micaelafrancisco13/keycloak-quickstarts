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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MySQLUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache
{
    private static final Logger logger = Logger.getLogger(MySQLUserStorageProvider.class);
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

    protected ComponentModel model;
    protected KeycloakSession session;

    protected EntityManager externalEntityManager;
    protected EntityManager testExternalEntityManager;
    protected EntityManager keycloakEntityManager;

    private String firstName;
    private String lastName;

    MySQLUserStorageProvider() {

    }

    MySQLUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        externalEntityManager = session.getProvider(JpaConnectionProvider.class, "custom-user-store").getEntityManager();
        keycloakEntityManager = session.getProvider(JpaConnectionProvider.class, "keycloak-user-store").getEntityManager();
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        UserEntity entity = getCurrentEntity(id);
        if (entity == null) {
            logger.info("could not find user by id: " + id);
            return null;
        }

        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        TypedQuery<UserEntity> query = externalEntityManager.createNamedQuery("getUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        List<UserEntity> result = query.getResultList();

        if (result.isEmpty()) {
            logger.info("could not find username: " + username);
            return null;
        }

        return new UserAdapter(session, realm, model, result.get(0));
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        TypedQuery<UserEntity> query = externalEntityManager.createNamedQuery("getUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        List<UserEntity> result = query.getResultList();

        if (result.isEmpty()) return null;

        return new UserAdapter(session, realm, model, result.get(0));
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        System.out.println("addUser");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedUtcDateTime = Instant.now().atZone(ZoneOffset.UTC).format(formatter);
        Timestamp timestampUtc = Timestamp.valueOf(formattedUtcDateTime);

        UserEntity userEntity = new UserEntity();

        userEntity.setUsername(username);
        userEntity.setEmail(username);
        userEntity.setCreatedAt(timestampUtc);
        userEntity.setNonNullPassword("");
        userEntity.setCompanyId(18);
        userEntity.setPartnerId((short) 1);
        userEntity.setWhoAdded(1);

        externalEntityManager.persist(userEntity);
        UserModel userAdapter = new UserAdapter(session, realm, model, userEntity);
//        jakarta.enterprise.inject.spi.CDI.current().getBeanManager().getEvent().fire(userAdapter);

        UserEntity currentUserEntity = getCurrentEntity(userAdapter.getId());
        System.out.println("userAdapter.getId() " + userAdapter.getId());
        System.out.println("persisted user entity's first name " + currentUserEntity.getFirstName());

        System.out.println("before returning");
        return userAdapter;
    }

    @Transactional(value = Transactional.TxType.MANDATORY)
    public void onUserCreated(@Observes(during = TransactionPhase.BEFORE_COMPLETION) UserModel user) {
        System.out.println("onUserCreated");
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserEntity entity = getCurrentEntity(user.getId());

        if (entity == null) return false;

        externalEntityManager.remove(entity);

        return true;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        String password = ((UserAdapter)delegate).getPassword();

        if (password != null) user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel userCredentialModel)) return false;
        UserAdapter adapter = getUserAdapter(user);

        adapter.setPassword(hashPassword(realm, userCredentialModel.getValue()));

        return true;
    }

    private String hashPassword(RealmModel realm, String password) {
        String algorithm = realm.getPasswordPolicy().getHashAlgorithm();
        int iterations = realm.getPasswordPolicy().getHashIterations();

        if (algorithm.equals("bcrypt"))
            return BCrypt.hashpw(password, BCrypt.gensalt(iterations));

        return "";
    }

    public UserAdapter getUserAdapter(UserModel user) {
        if (user instanceof CachedUserModel) return (UserAdapter)((CachedUserModel) user).getDelegateForUpdate();
        return (UserAdapter) user;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;

        getUserAdapter(user).setPassword(null);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        if (getUserAdapter(user).getPassword() != null) {
            Set<String> set = new HashSet<>();
            set.add(PasswordCredentialModel.TYPE);
            return set.stream();
        }
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel userModel, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel userCredentialModel)) return false;

        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            String rawPassword = userCredentialModel.getValue();
            String hashedPassword = getCurrentEntity(userModel.getId()).getPassword();

            return hashedPassword != null && BCrypt.checkpw(rawPassword, hashedPassword);
        }

        return false;
    }

    public String getPassword(UserModel user) {
        String password = null;

        if (user instanceof CachedUserModel)
            password = (String)((CachedUserModel)user).getCachedWith().get(PASSWORD_CACHE_KEY);
        else if (user instanceof UserAdapter)
            password = ((UserAdapter)user).getPassword();

        return password;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        Object count = externalEntityManager.createNamedQuery("getUserCount")
                .getSingleResult();

        return ((Number)count).intValue();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer pageNumber, Integer pageSize) {
        String search = params.get(UserModel.SEARCH);
        TypedQuery<UserEntity> query = externalEntityManager.createNamedQuery("searchForUser", UserEntity.class)
                .setParameter("search", "%" + search.toLowerCase() + "%");

        if (Objects.equals(search, "*"))
            query = externalEntityManager.createNamedQuery("getAllUsers", UserEntity.class);

        if (pageNumber != null)
            query.setFirstResult(pageNumber);
        if (pageSize != null)
            query.setMaxResults(pageSize);

        return query.getResultStream().map(entity -> new UserAdapter(session, realm, model, entity));
    }


    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    private UserEntity getCurrentEntity(String id) {
        String persistenceId = StorageId.externalId(id);

        return externalEntityManager.find(UserEntity.class, persistenceId);
    }
}
