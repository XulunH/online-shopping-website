package com.xulunh.accountservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="user_accounts",indexes = {
        @Index(name="ux_user_accounts_email",columnList = "email", unique = true)
})
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable=false, name="password_hash",length=255)
    private String password;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="line1",column=@Column(name="ship_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "ship_line2")),
            @AttributeOverride(name = "city", column = @Column(name = "ship_city")),
            @AttributeOverride(name = "state", column = @Column(name = "ship_state")),
            @AttributeOverride(name = "zip", column = @Column(name = "ship_zip")),
            @AttributeOverride(name = "country", column = @Column(name = "ship_country"))

    })
    private Address shipAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1", column = @Column(name = "bill_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "bill_line2")),
            @AttributeOverride(name = "city", column = @Column(name = "bill_city")),
            @AttributeOverride(name = "state", column = @Column(name = "bill_state")),
            @AttributeOverride(name = "zip", column = @Column(name = "bill_zip")),
            @AttributeOverride(name = "country", column = @Column(name = "bill_country"))
    })
    private Address billingAddress;

    @Column(name="created_at")
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
