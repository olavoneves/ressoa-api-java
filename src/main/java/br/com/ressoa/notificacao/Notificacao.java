package br.com.ressoa.notificacao;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.ressoa.avaliacao.AvaliacaoRisco;
import br.com.ressoa.shared.persistence.SimNaoConverter;
import br.com.ressoa.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Alerta ao morador quando o risco do bairro sobe (Feature 7). */
@Entity
@Table(name = "NOTIFICACAO")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AVALIACAO_ID", nullable = false)
    private AvaliacaoRisco avaliacao;

    @Column(name = "TITULO", nullable = false, length = 120)
    private String titulo;

    @Column(name = "MENSAGEM", nullable = false, length = 1000)
    private String mensagem;

    @Convert(converter = SimNaoConverter.class)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "LIDA", nullable = false, length = 1)
    private Boolean lida = Boolean.FALSE;

    @Column(name = "CRIADA_EM", nullable = false)
    private Instant criadaEm;

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, AvaliacaoRisco avaliacao, String titulo, String mensagem) {
        this.usuario = usuario;
        this.avaliacao = avaliacao;
        this.titulo = titulo;
        this.mensagem = mensagem;
    }

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
    }

    public void marcarComoLida() {
        this.lida = Boolean.TRUE;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public AvaliacaoRisco getAvaliacao() {
        return avaliacao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public boolean isLida() {
        return Boolean.TRUE.equals(lida);
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
