package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "provinces")
@Getter
@Setter
@ToString(exclude = {"districts"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceEntity {
    @EqualsAndHashCode.Include
    @Id
    private Long id;

    private String name;

    @Column(nullable = false)
    private String codename;

    @Column(nullable = false)
    private String divisionType;

    private Integer phoneCode;

    @Column(columnDefinition = "TEXT")
    private String image;

    @OneToMany(mappedBy = "province")
    private List<DistrictEntity> districts;

    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }
}
