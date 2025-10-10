package com.morapack.repository;

import com.morapack.model.T01Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface T01AeropuertoRepository extends JpaRepository<T01Aeropuerto, String> {
    Optional<T01Aeropuerto> findByT01Codigoicao(String t01Codigoicao);

    // Verificar existencia por IATA (para validación de duplicados)
    boolean existsByT01Codigoicao(String codigoIATA);

    // JPQL no soporta LIMIT, se usa Spring Data para obtener el primero ordenado descendentemente
    @Query("SELECT a.t0Idaeropuerto FROM T01Aeropuerto a ORDER BY a.t0Idaeropuerto DESC")
    List<String> findUltimoIdAeropuertoLista();

    // Helper para obtener el último ID como Optional<String>
    default Optional<String> findUltimoIdAeropuerto() {
        List<String> lista = findUltimoIdAeropuertoLista();
        return lista.isEmpty() ? Optional.empty() : Optional.ofNullable(lista.get(0));
    }

    // Opcional: Búsqueda por continente y GMT
    @Query("SELECT a FROM T01Aeropuerto a WHERE a.t01Continente = :continente AND a.t01GmtOffset = :gmtOffset")
    List<T01Aeropuerto> findByContinenteAndGmtOffset(@Param("continente") String continente, @Param("gmtOffset") Byte gmtOffset);
}
