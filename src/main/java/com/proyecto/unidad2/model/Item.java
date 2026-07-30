package com.proyecto.unidad2.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("items")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Item {

    @Id
    private Long id;

    @Column("titulo")
    private String titulo;

    @Column("plataforma")
    private String plataforma;

    @Column("precio")
    private Double precio;
}