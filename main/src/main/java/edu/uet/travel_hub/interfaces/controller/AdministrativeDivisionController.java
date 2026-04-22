package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.response.AdminDistrictResponse;
import edu.uet.travel_hub.application.dto.response.AdminProvinceResponse;
import edu.uet.travel_hub.application.dto.response.AdminWardResponse;
import edu.uet.travel_hub.application.usecases.AdministrativeDivisionService;

@RestController
@RequestMapping("/api/locations")
public class AdministrativeDivisionController {
    private final AdministrativeDivisionService administrativeDivisionService;

    public AdministrativeDivisionController(AdministrativeDivisionService administrativeDivisionService) {
        this.administrativeDivisionService = administrativeDivisionService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<AdminProvinceResponse>> getProvinces() {
        return ResponseEntity.ok(this.administrativeDivisionService.getProvinces());
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<List<AdminDistrictResponse>> getDistricts(@PathVariable Long provinceId) {
        return ResponseEntity.ok(this.administrativeDivisionService.getDistrictsByProvince(provinceId));
    }

    @GetMapping("/districts/{districtId}/wards")
    public ResponseEntity<List<AdminWardResponse>> getWards(@PathVariable Long districtId) {
        return ResponseEntity.ok(this.administrativeDivisionService.getWardsByDistrict(districtId));
    }
}
