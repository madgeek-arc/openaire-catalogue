/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.invitations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String inviterEmail;
    private byte[] inviteeHash;
    private Date expiration;

    public Invitation() {
        // no-arg constructor
    }

    public Invitation(String inviterEmail, byte[] inviteeHash, Date expiration) {
        this.inviterEmail = inviterEmail;
        this.inviteeHash = inviteeHash;
        this.expiration = expiration;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInviterEmail() {
        return inviterEmail;
    }

    public void setInviterEmail(String inviterEmail) {
        this.inviterEmail = inviterEmail;
    }

    public byte[] getInviteeHash() {
        return inviteeHash;
    }

    public void setInviteeHash(byte[] inviteeHash) {
        this.inviteeHash = inviteeHash;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    @Override
    // have used getTime() method on expiration Date field to avoid mismatch with DB saved format
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invitation that = (Invitation) o;
        return Objects.equals(id, that.id) && Objects.equals(inviterEmail, that.inviterEmail) && Arrays.equals(inviteeHash, that.inviteeHash) && Objects.equals(expiration.getTime(), that.expiration.getTime());
    }

    @Override
    // have used getTime() method on expiration Date field to avoid mismatch with DB saved format
    public int hashCode() {
        int result = Objects.hash(id, inviterEmail, expiration.getTime());
        result = 31 * result + Arrays.hashCode(inviteeHash);
        return result;
    }
}
