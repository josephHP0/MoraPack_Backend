package com.morapack.nuevomoraback.common.service;

import com.morapack.nuevomoraback.common.domain.T01Aeropuerto;
import com.morapack.nuevomoraback.common.domain.T05Ciudad;
import com.morapack.nuevomoraback.common.dto.AeropuertoDTO;
import com.morapack.nuevomoraback.common.repository.AeropuertoRepository;
import com.morapack.nuevomoraback.common.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AeropuertoServiceImpl implements AeropuertoService {

    private final AeropuertoRepository aeropuertoRepository;
    private final CiudadRepository ciudadRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AeropuertoDTO> obtenerTodosLosAeropuertos() {
        log.info("Obteniendo todos los aeropuertos");

        List<T01Aeropuerto> aeropuertos = aeropuertoRepository.findAll();

        // Crear mapa de ciudades para determinar si es hub
        Map<Integer, Boolean> ciudadesHub = ciudadRepository.findAll().stream()
            .collect(Collectors.toMap(T05Ciudad::getId, c -> c.getT05EsHub() != null && c.getT05EsHub()));

        List<AeropuertoDTO> resultado = aeropuertos.stream()
            .map(a -> convertirADTO(a, ciudadesHub.getOrDefault(a.getT01IdCiudad(), false)))
            .collect(Collectors.toList());

        log.info("Total de aeropuertos obtenidos: {}", resultado.size());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AeropuertoDTO> obtenerAeropuertosHub() {
        log.info("Obteniendo aeropuertos HUB");

        List<T01Aeropuerto> aeropuertos = aeropuertoRepository.findAeropuertosHub();

        List<AeropuertoDTO> resultado = aeropuertos.stream()
            .map(a -> convertirADTO(a, true))
            .collect(Collectors.toList());

        log.info("Total de aeropuertos HUB: {}", resultado.size());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AeropuertoDTO> obtenerAeropuertosNoHub() {
        log.info("Obteniendo aeropuertos NO-HUB");

        List<T01Aeropuerto> aeropuertos = aeropuertoRepository.findAeropuertosNoHub();

        List<AeropuertoDTO> resultado = aeropuertos.stream()
            .map(a -> convertirADTO(a, false))
            .collect(Collectors.toList());

        log.info("Total de aeropuertos NO-HUB: {}", resultado.size());
        return resultado;
    }

    /**
     * Convierte una entidad T01Aeropuerto a AeropuertoDTO
     */
    private AeropuertoDTO convertirADTO(T01Aeropuerto aeropuerto, Boolean esHub) {
        return AeropuertoDTO.builder()
            .id(aeropuerto.getId())
            .codigoICAO(aeropuerto.getT01CodigoIcao())
            .nombre(aeropuerto.getT01Alias())
            .latitud(aeropuerto.getT01Lat())
            .longitud(aeropuerto.getT01Lon())
            .gmtOffset(aeropuerto.getT01GmtOffset())
            .capacidad(aeropuerto.getT01Capacidad())
            .esHub(esHub)
            .idCiudad(aeropuerto.getT01IdCiudad())
            .build();
    }
}
