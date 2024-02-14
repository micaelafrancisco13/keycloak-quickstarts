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


import jakarta.persistence.*;

import java.sql.Timestamp;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getUserByUsername", query="select u from UserEntity u WHERE u.username = :username"),
        @NamedQuery(name="getUserByEmail", query="select u from UserEntity u WHERE u.username = :email"),
        @NamedQuery(name="getUserCount", query="select count(u) from UserEntity u"),
        @NamedQuery(name="getAllUsers", query="select u from UserEntity u"),
        @NamedQuery(name= "getLastSyncDate", query = "SELECT MAX(u.lastSyncDate) FROM UserEntity u"),
        @NamedQuery(name= "getUsersChangedSince",
                query = "SELECT u FROM UserEntity u WHERE u.lastModifiedDate >= :lastSync "),
        @NamedQuery(name="searchForUser", query="select u from UserEntity u WHERE " + "( lower(u.username) like :search or u.username like :search ) order by u.username"),
})
@Entity
@Table(name = "User")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Integer id;

    @Column(name = "whenAdded")
    private Timestamp createdAt;

    @Column(name = "ts")
    private Timestamp lastModifiedDate;

    @Column(name = "last_sync_date")
    private Timestamp lastSyncDate;

    @Column(name = "userName")
    private String username;

    private String email;

    private String firstName;

    private String lastName;

    @Column(name = "bCryptPassword")
    private String password;

    private String status;

    @Column(name = "phoneMobile")
    private String mobilePhone;

    @Column(name = "phoneOffice")
    private String officePhone;

    @Column(name = "password")
    private String nonNullPassword;

    private int companyId;

    private short partnerId;

    private int whoAdded;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Timestamp lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Timestamp getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Timestamp lastSyncDate) {
        System.out.println("lastSyncDate " + lastSyncDate);
        this.lastSyncDate = lastSyncDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getOfficePhone() {
        return officePhone;
    }

    public void setOfficePhone(String officePhone) {
        this.officePhone = officePhone;
    }

    public String getNonNullPassword() {
        return nonNullPassword;
    }

    public void setNonNullPassword(String nonNullPassword) {
        this.nonNullPassword = nonNullPassword;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public short getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(short partnerId) {
        this.partnerId = partnerId;
    }

    public int getWhoAdded() {
        return whoAdded;
    }

    public void setWhoAdded(int whoAdded) {
        this.whoAdded = whoAdded;
    }
}
