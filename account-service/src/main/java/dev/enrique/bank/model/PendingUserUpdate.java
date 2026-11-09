package dev.enrique.bank.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A status or email change of a user whose account does not exist yet. Each topic of user-service is
 * consumed on its own, so such a change can arrive before the account-requested event of the same
 * user; it is kept here and applied when the account is opened. Null fields did not change.
 */
@Entity
@Table(name = "pending_user_update")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingUserUpdate {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "email")
    private String email;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PendingUserUpdate(String userId) {
        this.userId = userId;
    }
}
