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

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.quickstart.storage.user.enums.UserAttributes;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter extends AbstractUserAdapterFederatedStorage {
    private static final Logger logger = Logger.getLogger(UserAdapter.class);

    protected UserEntity userEntity;

    protected UserEntityTest userEntityTest;

    protected String keycloakId;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity userEntity) {
        super(session, realm, model);
        this.userEntity = userEntity;
        keycloakId = StorageId.keycloakId(model, String.valueOf(userEntity.getId()));
    }

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntityTest userEntityTest) {
        super(session, realm, model);
        this.userEntityTest = userEntityTest;
        keycloakId = StorageId.keycloakId(model, String.valueOf(userEntityTest.getId()));
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public String getUsername() {
        return userEntity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        userEntity.setUsername(username);
    }

    @Override
    public String getEmail() {
        return userEntity.getEmail();
    }

    @Override
    public void setEmail(String email) {
        userEntity.setEmail(email);
        super.setEmail(email);
    }

//    @Override
//    public String getFirstName() {
//        return userEntity.getFirstName();
//    }
//
//    @Override
//    public void setFirstName(String firstName) {
//        userEntity.setFirstName(firstName);
//        super.setFirstName(firstName);
//    }
//
//    @Override
//    public String getLastName() {
//        return userEntity.getLastName();
//    }
//
//    @Override
//    public void setLastName(String lastName) {
//        userEntity.setLastName(lastName);
//        super.setLastName(lastName);
//    }

    public String getPassword() {
        return userEntity.getPassword();
    }

    public void setPassword(String password) {
        userEntity.setPassword(password);
    }

    @Override
    public Long getCreatedTimestamp() {
        return userEntity.getCreatedAt().getTime();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        userEntity.setCreatedAt(new Timestamp(timestamp));
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        logger.info(" > > > setSingleAttribute");

        if (name.equals(String.valueOf(UserAttributes.STATUS)))
            userEntity.setStatus(value);
        else if (name.equals(String.valueOf(UserAttributes.MOBILE_PHONE)))
            userEntity.setMobilePhone(value);
        else if (name.equals(String.valueOf(UserAttributes.OFFICE_PHONE)))
            userEntity.setOfficePhone(value);
        else
            super.setSingleAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        logger.info(" > > > removeAttribute");

        if (name.equals(String.valueOf(UserAttributes.STATUS)))
            userEntity.setStatus(null);
        else if (name.equals(String.valueOf(UserAttributes.MOBILE_PHONE)))
            userEntity.setMobilePhone(null);
        else if (name.equals(String.valueOf(UserAttributes.OFFICE_PHONE)))
            userEntity.setOfficePhone(null);
        else
            super.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        logger.info(" > > > setAttribute");

        if (name.equals(String.valueOf(UserAttributes.STATUS)))
            userEntity.setStatus(values.get(0));
        else if (name.equals(String.valueOf(UserAttributes.MOBILE_PHONE)))
            userEntity.setMobilePhone(values.get(0));
        else if (name.equals(String.valueOf(UserAttributes.OFFICE_PHONE)))
            userEntity.setOfficePhone(values.get(0));
        else
            super.setAttribute(name, values);
    }

    @Override
    public String getFirstAttribute(String name) {
        logger.info(" > > > getFirstAttribute");

        if (name.equals(String.valueOf(UserAttributes.STATUS)))
            return userEntity.getStatus();
        else if (name.equals(String.valueOf(UserAttributes.MOBILE_PHONE)))
            return userEntity.getMobilePhone();
        else if (name.equals(String.valueOf(UserAttributes.OFFICE_PHONE)))
            return userEntity.getOfficePhone();
        else
            return super.getFirstAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        logger.info(" > > > getAttributes");

        Map<String, List<String>> attrs = super.getAttributes();

        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);

        all.add(String.valueOf(UserAttributes.STATUS), userEntity.getStatus());
        all.add(String.valueOf(UserAttributes.MOBILE_PHONE), userEntity.getMobilePhone());
        all.add(String.valueOf(UserAttributes.OFFICE_PHONE), userEntity.getOfficePhone());

        return all;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        logger.info(" > > > getAttributeStream");

        if (name.equals(String.valueOf(UserAttributes.STATUS))) {
            List<String> status = new LinkedList<>();
            status.add(userEntity.getStatus());
            return status.stream();
        } else if (name.equals(String.valueOf(UserAttributes.MOBILE_PHONE))) {
            List<String> phone = new LinkedList<>();
            phone.add(userEntity.getMobilePhone());
            return phone.stream();
        } else if (name.equals(String.valueOf(UserAttributes.OFFICE_PHONE))) {
            List<String> officePhone = new LinkedList<>();
            officePhone.add(userEntity.getOfficePhone());
            return officePhone.stream();
        } else
            return super.getAttributeStream(name);
    }
}
