package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuarios_username", columnNames = {"username"}),
                @UniqueConstraint(name = "uk_usuarios_email", columnNames = {"email"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "number", nullable = false, length = 100)
    private String number;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // antes
// @ManyToOne(fetch = FetchType.LAZY)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "rol_id",
            foreignKey = @ForeignKey(name = "fk_usuario_rol")
    )
    private Rol rol;
}
