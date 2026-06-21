package com.subastas.service;

import com.subastas.entity.Multa;
import com.subastas.repository.MultaRepository;
import com.subastas.repository.VictoriaPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MultaService {

    private final VictoriaPagoRepository victoriaRepo;
    private final MultaRepository multaRepo;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void generarMultas() {
        var limite = LocalDateTime.now().minusHours(72);
        var vencidas = victoriaRepo.findByPagadoAndFechavictoriaBefore("no", limite);

        for (var v : vencidas) {
            if (multaRepo.existsByRegistro(v.getRegistro())) continue;

            Multa multa = new Multa();
            multa.setRegistro(v.getRegistro());
            multa.setCliente(v.getCliente());
            multa.setImporte(v.getImporte().multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP));
            multa.setMotivo("Falta de pago en 72hs");
            multa.setPagada("no");
            multa.setFechamulta(LocalDateTime.now());
            multaRepo.save(multa);
        }
    }
}
