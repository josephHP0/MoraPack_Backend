package com.morapack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class T02AirportInventoryId implements Serializable {
    private static final long serialVersionUID = -1144377768991778720L;
    @NotNull
    @Column(name = "T02_idAeropuerto", nullable = false)
    private Integer t02Idaeropuerto;

    @NotNull
    @Column(name = "T03_idPedido", nullable = false)
    private Integer t03Idpedido;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        T02AirportInventoryId entity = (T02AirportInventoryId) o;
        return Objects.equals(this.t02Idaeropuerto, entity.t02Idaeropuerto) &&
                Objects.equals(this.t03Idpedido, entity.t03Idpedido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(t02Idaeropuerto, t03Idpedido);
    }

}