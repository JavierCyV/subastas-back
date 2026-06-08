package com.subastas.repository;

import com.subastas.entity.VictoriaPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VictoriaPagoRepository extends JpaRepository<VictoriaPago, Integer> {
    List<VictoriaPago> findByClienteAndPagado(Integer clienteId, String pagado);
    List<VictoriaPago> findByPagadoAndFechavictoriaBefore(String pagado, LocalDateTime fecha);
    boolean existsByRegistro(Integer registroId);
}
