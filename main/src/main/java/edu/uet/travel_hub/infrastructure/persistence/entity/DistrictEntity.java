package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "districts")
@Getter
@Setter
@ToString(exclude = {"province", "wards"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictEntity {
    @EqualsAndHashCode.Include
    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private ProvinceEntity province;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String codename;

    @Column(nullable = false)
    private String divisionType;

    @OneToMany(mappedBy = "district")
    private List<WardEntity> wards;

    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }
}
