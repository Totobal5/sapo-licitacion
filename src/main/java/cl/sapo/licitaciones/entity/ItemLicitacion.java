package cl.sapo.licitaciones.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a tender item (producto/servicio).
 * Child entity of Licitacion.
 */
@Entity
@Table(name = "tender_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemLicitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_code", nullable = false)
    private Licitacion licitacion;

    @Override
    public String toString() {
        return "ItemLicitacion{" +
                "id=" + id +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
