package cl.sapo.licitaciones.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a public tender (licitación).
 * Flattens the nested API structure with region extracted from Comprador.RegionUnidad.
 */
@Entity
@Table(name = "tenders", indexes = {
        @Index(name = "idx_tender_code", columnList = "external_code"),
        @Index(name = "idx_tender_status", columnList = "status_code"),
        @Index(name = "idx_tender_region", columnList = "region"),
        @Index(name = "idx_tender_close_date", columnList = "close_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Licitacion {

    @Id
    @Column(name = "external_code", nullable = false, unique = true)
    private String codigoExterno;

    @Column(name = "name", nullable = false, length = 500)
    private String nombre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "status_code", nullable = false)
    private Integer codigoEstado;

    @Column(name = "close_date")
    private LocalDateTime fechaCierre;

    @Column(name = "publication_date")
    private LocalDateTime fechaPublicacion;

    /**
     * Region extracted from Comprador.RegionUnidad in the API response.
     * Used for filtering tenders by region.
     */
    @Column(name = "region")
    private String region;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_rut")
    private String buyerRut;

    @OneToMany(mappedBy = "licitacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemLicitacion> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method to add an item and maintain bidirectional relationship.
     */
    public void addItem(ItemLicitacion item) {
        items.add(item);
        item.setLicitacion(this);
    }

    /**
     * Helper method to remove an item and maintain bidirectional relationship.
     */
    public void removeItem(ItemLicitacion item) {
        items.remove(item);
        item.setLicitacion(null);
    }

    @Override
    public String toString() {
        return "Licitacion{" +
                "codigoExterno='" + codigoExterno + '\'' +
                ", nombre='" + nombre + '\'' +
                ", region='" + region + '\'' +
                ", codigoEstado=" + codigoEstado +
                '}';
    }
}
