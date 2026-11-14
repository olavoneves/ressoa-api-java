package br.com.ressoa.usuario;

import java.time.Instant;

import br.com.ressoa.bairro.Bairro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Morador ou gestor. LGPD: só nome, e-mail e bairro. */
@Entity
@Table(name = "USUARIO")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @Column(name = "EMAIL", nullable = false, length = 160, unique = true)
    private String email;

    @Column(name = "SENHA_HASH", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAPEL", nullable = false, length = 10)
    private Papel papel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BAIRRO_ID")
    private Bairro bairro;

    @Column(name = "CRIADO_EM", nullable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, Papel papel, Bairro bairro) {
        this.nome = nome.trim();
        this.email = email.trim().toLowerCase();
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.bairro = bairro;
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public boolean isGestor() {
        return papel == Papel.GESTOR;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public Bairro getBairro() {
        return bairro;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
