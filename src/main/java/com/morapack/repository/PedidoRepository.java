// PedidoRepository.java
package com.morapack.repository;
import com.morapack.model.T03Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface PedidoRepository extends JpaRepository<T03Pedido, Integer> {
    // findByCodigo, findByDestinoIata, etc.
}
