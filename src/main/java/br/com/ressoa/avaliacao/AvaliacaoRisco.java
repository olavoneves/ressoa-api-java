package br.com.ressoa.avaliacao;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.NivelRisco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Avaliação semanal do risco de dengue de um bairro, gravada pelo serviço de IA ou pelo gestor.
 * Guarda os indicadores que justificam o nível (explicabilidade) e a orientação gerada por IA.
 */
@Entity
@Table(name = "AVALIACAO_RISCO")
public class AvaliacaoRisco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BAIRRO_ID", nullable = false)
    private Bairro bairro;

    /** Formato AAAASS, ex.: 202647 (padrão do InfoDengue). */
    @Column(name = "SEMANA_EPIDEMIOLOGICA", nullable = false)
    private Integer semanaEpidemiologica;

    @Enumerated(EnumType.STRING)
    @Column(name = "NIVEL", nullable = false, length = 10)
    private NivelRisco nivel;

    @Column(name = "TEMP_MIN_MEDIA", precision = 4, scale = 1)
    private BigDecimal tempMinMedia;

    @Column(name = "CASOS_100K", precision = 8, scale = 2)
    private BigDecimal casos100k;

    @Column(name = "RT", precision = 5, scale = 2)
    private BigDecimal rt;

    @Lob
    @Column(name = "ORIENTACAO_IA")
    private String orientacaoIa;

    @Column(name = "CRIADA_EM", nullable = false)
    private Instant criadaEm;

    protected AvaliacaoRisco() {
    }

    public AvaliacaoRisco(Bairro bairro, int semanaEpidemiologica, NivelRisco nivel, BigDecimal tempMinMedia,
            BigDecimal casos100k, BigDecimal rt) {
        this.bairro = bairro;
        this.semanaEpidemiologica = semanaEpidemiologica;
        this.nivel = nivel;
        this.tempMinMedia = tempMinMedia;
        this.casos100k = casos100k;
        this.rt = rt;
    }

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
    }

    public void registrarOrientacao(String texto) {
        this.orientacaoIa = texto;
    }

    public Long getId() {
        return id;
    }

    public Bairro getBairro() {
        return bairro;
    }

    public Integer getSemanaEpidemiologica() {
        return semanaEpidemiologica;
    }

    public NivelRisco getNivel() {
        return nivel;
    }

    public BigDecimal getTempMinMedia() {
        return tempMinMedia;
    }

    public BigDecimal getCasos100k() {
        return casos100k;
    }

    public BigDecimal getRt() {
        return rt;
    }

    public String getOrientacaoIa() {
        return orientacaoIa;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
