package br.com.ressoa.dispositivo;

import java.time.Instant;

import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.usuario.Usuario;
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

/** Aparelho Ressoa de ultrassom instalado num recipiente de água. */
@Entity
@Table(name = "DISPOSITIVO")
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NUMERO_SERIE", nullable = false, length = 40, unique = true)
    private String numeroSerie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BAIRRO_ID", nullable = false)
    private Bairro bairro;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_RECIPIENTE", nullable = false, length = 12)
    private TipoRecipiente tipoRecipiente;

    @Column(name = "VOLUME_LITROS", nullable = false)
    private Integer volumeLitros;

    @Column(name = "PULSOS_POR_DIA", nullable = false)
    private Integer pulsosPorDia = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    private StatusDispositivo status = StatusDispositivo.ATIVO;

    @Column(name = "INSTALADO_EM", nullable = false)
    private Instant instaladoEm;

    protected Dispositivo() {
    }

    public Dispositivo(String numeroSerie, Usuario usuario, Bairro bairro, TipoRecipiente tipoRecipiente,
            int volumeLitros, int pulsosPorDia) {
        this.numeroSerie = numeroSerie.trim().toUpperCase();
        this.usuario = usuario;
        this.bairro = bairro;
        this.tipoRecipiente = tipoRecipiente;
        this.volumeLitros = volumeLitros;
        this.pulsosPorDia = pulsosPorDia;
    }

    @PrePersist
    void aoCriar() {
        if (instaladoEm == null) {
            instaladoEm = Instant.now();
        }
    }

    public void atualizar(Bairro bairro, TipoRecipiente tipoRecipiente, int volumeLitros, int pulsosPorDia,
            StatusDispositivo status) {
        this.bairro = bairro;
        this.tipoRecipiente = tipoRecipiente;
        this.volumeLitros = volumeLitros;
        this.pulsosPorDia = pulsosPorDia;
        if (status != null) {
            this.status = status;
        }
    }

    public boolean pertenceA(Long usuarioId) {
        return usuario.getId().equals(usuarioId);
    }

    public Long getId() {
        return id;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Bairro getBairro() {
        return bairro;
    }

    public TipoRecipiente getTipoRecipiente() {
        return tipoRecipiente;
    }

    public Integer getVolumeLitros() {
        return volumeLitros;
    }

    public Integer getPulsosPorDia() {
        return pulsosPorDia;
    }

    public StatusDispositivo getStatus() {
        return status;
    }

    public Instant getInstaladoEm() {
        return instaladoEm;
    }
}
