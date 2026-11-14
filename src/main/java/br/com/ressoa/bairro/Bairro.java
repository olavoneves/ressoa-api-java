package br.com.ressoa.bairro;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.ressoa.shared.persistence.SimNaoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "BAIRRO")
public class Bairro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @Column(name = "MUNICIPIO", nullable = false, length = 120)
    private String municipio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "UF", nullable = false, length = 2)
    private String uf;

    @Column(name = "CODIGO_IBGE", nullable = false, length = 7)
    private String codigoIbge;

    @Convert(converter = SimNaoConverter.class)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "AGUA_INTERMITENTE", nullable = false, length = 1)
    private Boolean aguaIntermitente = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "NIVEL_RISCO", nullable = false, length = 10)
    private NivelRisco nivelRisco = NivelRisco.VERDE;

    protected Bairro() {
    }

    public Bairro(String nome, String municipio, String uf, String codigoIbge, boolean aguaIntermitente) {
        atualizar(nome, municipio, uf, codigoIbge, aguaIntermitente);
    }

    public final void atualizar(String nome, String municipio, String uf, String codigoIbge,
            boolean aguaIntermitente) {
        this.nome = nome.trim();
        this.municipio = municipio.trim();
        this.uf = uf.trim().toUpperCase();
        this.codigoIbge = codigoIbge.trim();
        this.aguaIntermitente = aguaIntermitente;
    }

    public void alterarNivelRisco(NivelRisco nivel) {
        this.nivelRisco = nivel;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getMunicipio() {
        return municipio;
    }

    public String getUf() {
        return uf;
    }

    public String getCodigoIbge() {
        return codigoIbge;
    }

    public boolean isAguaIntermitente() {
        return Boolean.TRUE.equals(aguaIntermitente);
    }

    public NivelRisco getNivelRisco() {
        return nivelRisco;
    }
}
