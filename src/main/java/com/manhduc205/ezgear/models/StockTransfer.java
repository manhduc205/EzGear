package com.manhduc205.ezgear.models;

import com.manhduc205.ezgear.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer extends AbstractEntity {

    @Column(unique = true, nullable = false)
    private String code; // ST-xxxx

    @ManyToOne
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;

    @ManyToOne
    @JoinColumn(name = "to_warehouse_id")
    private Warehouse toWarehouse;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    private String note;

    @Column(name = "reference_code")
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL)
    private List<StockTransferItem> items;

}