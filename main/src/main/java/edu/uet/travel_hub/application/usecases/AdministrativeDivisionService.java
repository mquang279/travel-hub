package edu.uet.travel_hub.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.response.AdminDistrictResponse;
import edu.uet.travel_hub.application.dto.response.AdminProvinceResponse;
import edu.uet.travel_hub.application.dto.response.AdminWardResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.infrastructure.persistence.entity.DistrictEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ProvinceEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.WardEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.DistrictJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ProvinceJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.WardJpaRepository;

@Service
public class AdministrativeDivisionService {
    private final ProvinceJpaRepository provinceJpaRepository;
    private final DistrictJpaRepository districtJpaRepository;
    private final WardJpaRepository wardJpaRepository;

    public AdministrativeDivisionService(
            ProvinceJpaRepository provinceJpaRepository,
            DistrictJpaRepository districtJpaRepository,
            WardJpaRepository wardJpaRepository) {
        this.provinceJpaRepository = provinceJpaRepository;
        this.districtJpaRepository = districtJpaRepository;
        this.wardJpaRepository = wardJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminProvinceResponse> getProvinces() {
        return this.provinceJpaRepository.findAllByOrderByNameAsc().stream()
                .map(this::toProvinceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDistrictResponse> getDistrictsByProvince(Long provinceId) {
        ensureProvinceExists(provinceId);
        return this.districtJpaRepository.findByProvinceIdOrderByNameAsc(provinceId).stream()
                .map(this::toDistrictResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminWardResponse> getWardsByDistrict(Long districtId) {
        ensureDistrictExists(districtId);
        return this.wardJpaRepository.findByDistrictIdOrderByNameAsc(districtId).stream()
                .map(this::toWardResponse)
                .toList();
    }

    private AdminProvinceResponse toProvinceResponse(ProvinceEntity province) {
        return new AdminProvinceResponse(
                province.getId(),
                province.getName(),
                province.getCodename(),
                province.getDivisionType(),
                province.getPhoneCode(),
                province.getImage());
    }

    private AdminDistrictResponse toDistrictResponse(DistrictEntity district) {
        return new AdminDistrictResponse(
                district.getId(),
                district.getProvince().getId(),
                district.getName(),
                district.getCodename(),
                district.getDivisionType());
    }

    private AdminWardResponse toWardResponse(WardEntity ward) {
        return new AdminWardResponse(
                ward.getId(),
                ward.getDistrict().getId(),
                ward.getProvince().getId(),
                ward.getName(),
                ward.getCodename(),
                ward.getDivisionType());
    }

    private void ensureProvinceExists(Long provinceId) {
        if (!this.provinceJpaRepository.existsById(provinceId)) {
            throw new ResourceNotFoundException("Province not found");
        }
    }

    private void ensureDistrictExists(Long districtId) {
        if (!this.districtJpaRepository.existsById(districtId)) {
            throw new ResourceNotFoundException("District not found");
        }
    }
}
