package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_bancarios")
public class MovimientoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunidad_id")
    private Long comunidadId;

    @Column(name = "fecha_operacion")
    private LocalDate fechaOperacion;

    @Column(name = "fecha_valor")
    private LocalDate fechaValor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(length = 1)
    private String signo;

    @Column(length = 500)
    private String concepto;

    @Column(name = "concepto_completo", length = 500)
    private String conceptoCompleto;

    @Column(name = "referencia_bancaria", length = 50)
    private String referenciaBancaria;

    private Boolean procesado = false;

    private Boolean conciliado = false;

    @Column(name = "documento_extra", length = 255)
    private String documentoExtra;

    public Long getId() { return id; }

    public Long getComunidadId() { return comunidadId; }
    public void setComunidadId(Long comunidadId) { this.comunidadId = comunidadId; }

    public LocalDate getFechaOperacion() { return fechaOperacion; }
    public void setFechaOperacion(LocalDate fechaOperacion) { this.fechaOperacion = fechaOperacion; }

    public LocalDate getFechaValor() { return fechaValor; }
    public void setFechaValor(LocalDate fechaValor) { this.fechaValor = fechaValor; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public String getSigno() { return signo; }
    public void setSigno(String signo) { this.signo = signo; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getConceptoCompleto() { return conceptoCompleto; }
    public void setConceptoCompleto(String conceptoCompleto) { this.conceptoCompleto = conceptoCompleto; }

    public String getReferenciaBancaria() { return referenciaBancaria; }
    public void setReferenciaBancaria(String referenciaBancaria) { this.referenciaBancaria = referenciaBancaria; }

    public Boolean getProcesado() { return procesado; }
    public void setProcesado(Boolean procesado) { this.procesado = procesado; }

    public Boolean getConciliado() { return conciliado; }
    public void setConciliado(Boolean conciliado) { this.conciliado = conciliado; }

    public String getDocumentoExtra() { return documentoExtra; }
    public void setDocumentoExtra(String documentoExtra) { this.documentoExtra = documentoExtra; }
}